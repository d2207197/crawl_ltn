package cc.nlplab

import java.io._
import com.typesafe.scalalogging.slf4j._
import io.Source
import util.matching.Regex
import org.openqa.selenium.htmlunit.HtmlUnitDriver


case class Article(url: String, title:String, content:String )
case class Config(outputDir: File = new File("."), pageStart: Int = 1, pageEnd: Int = 212)

object Main extends LazyLogging {

// val logger = Logger(LoggerFactory getLogger "name")


  val parser = new scopt.OptionParser[Config]("crawl_ltn") {
    head("crawl_ltn", "1.0")
    help("help") text("prints this usage text")
    arg[File]("output_dir") optional() action { (x, c) =>
      c.copy(outputDir = x) } text("output dir. defaults to .") validate { x => if (x.isDirectory) success else failure("[output_dir] must be a directory")}
    arg[Int]("page_start") optional() action { (x, c) =>
      c.copy(pageStart = x) } text("page start. defaults to 1") 
    arg[Int]("page_end") optional() action { (x, c) =>
      c.copy(pageEnd = x) } text("page end. defaults to 212") 
  }

  def main(args: Array[String]) {
    parser.parse(args, Config()) map { config =>
      crawlLTN(config.outputDir, config.pageStart, config.pageEnd)
    } getOrElse {
      parser.showUsage
      System.exit(1) 
    }
  }

  def crawlLTN(output_dir: File, pageStart: Int, pageEnd: Int) = {
    val baseUrl = "http://iservice.ltn.com.tw/Service/english/"
    val urls = (for (page <- pageStart to pageEnd) yield s"${baseUrl}index.php?page=${page}").toList
    val articleUrlPattern = """english.php\?engno=[^"]*""".r

    val articleUrls = extractAllUrls(urls, baseUrl, articleUrlPattern)
    for (articleUrl <- articleUrls) {
      val pattern = "engno=([0-9]+)".r
      retry(5)(extractArticle(articleUrl)) match {
        case Some(article) =>
          val id = pattern.findFirstMatchIn(article.url).get.group(1)
          writeArticleToFile(article, new File(output_dir, id))
        case None => logger.error("Fetch Failed: ${articleUrl}")
      }
    }
  }

  def writeArticleToFile(article: Article, file: File) = {
    val pw = new PrintWriter(file)
    pw.write(s"URL:\t${article.url}\n")
    pw.write(s"TITLE:\t${article.title}\n\n")
    pw.write(article.content)
    pw.write("\n")
    pw.close()
  }

  @annotation.tailrec
  def retry[T](n: Int)(func: => Option[T] ): Option[T] = {
    func match {
      case some @ Some(_) => some
      case None =>
        if (n > 1) {
          logger.info(s"Retry ...")
          retry(n - 1)(func)
        } else None
    }
  }

  def extractArticle(url: String ): Option[Article] = {
    val driver = new HtmlUnitDriver
    logger.info(s"Fetching: ${url}")
    Thread.sleep(500)
    try {
      driver.get(url)
      Some(Article(
        url,
        driver.findElementByXPath("""//div[@class='title']""").getText,
        driver.findElementById("newsContent").getText
      ))
    } catch {
      case _: Throwable => None
    }
  }

  def getWebpage(url: String): String = {
    Thread.sleep(200)
    logger.info(s"Fetching: ${url}")
    Source.fromURL(url).mkString
  }

  def extractUrls(url: String, pattern: Regex): List[String] = 
    pattern.findAllIn(getWebpage(url)).toList

  def extractAllUrls(urls: List[String], baseUrl :String, pattern: Regex): List[String] =
    urls.flatMap(extractUrls(_, pattern)).map(baseUrl + _)

}
