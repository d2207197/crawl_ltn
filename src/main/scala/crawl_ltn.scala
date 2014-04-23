package cc.nlplab
import java.io._
import com.typesafe.scalalogging.slf4j._


case class Article(url: String, title:String, content:String )
case class Config(output_dir: File = new File("."))

object Main extends LazyLogging {

  import io.Source

  import util.matching.Regex

  import org.openqa.selenium.htmlunit.HtmlUnitDriver

// val logger = Logger(LoggerFactory getLogger "name")


  val parser = new scopt.OptionParser[Config]("crawl_ltn") {
    head("crawl_ltn", "1.0")
    help("help") text("prints this usage text")
    arg[File]("output_dir") optional() action { (x, c) =>
      c.copy(output_dir = x) } text("output dir") validate { x => if (x.isDirectory) success else failure("[output_dir] must be a directory")}
  }

  // val pages = 1 to 212
  def main(args: Array[String]) {
    parser.parse(args, Config()) map { config =>

      println(config)
      // System.exit(0)

      val baseUrl = "http://iservice.ltn.com.tw/Service/english/"
      val urls = (for (page <- 1 to 212) yield s"${baseUrl}index.php?page=${page}").toList
      val articleUrlPattern = """english.php\?engno=[^"]*""".r
      val articleUrls = extractAllUrls(urls, baseUrl, articleUrlPattern)

      for (articleUrl <- articleUrls) {
        val pattern = "engno=([0-9]+)".r
        extractArticle(articleUrl) match {
          case Some(article) =>
            val id = pattern.findFirstMatchIn(article.url).get.group(1)
            writeArticleToFile(article, new File(config.output_dir, id))
          case None => logger.error("Fetch Failed: ${articleUrl}")
        }
      }

    } getOrElse {
      parser.showUsage
      System.exit(1) 
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

  // @annotation.tailrec
  def extractArticle(url: String, retry_count: Int = 0): Option[Article] = {
    val driver = new HtmlUnitDriver
    logger.info(s"Fetching(${retry_count}): ${url}")
    Thread.sleep(500)
    try {
      driver.get(url)
    } catch {
      case e: java.net.SocketException if retry_count < 10 =>
        extractArticle(url, retry_count + 1)
      case _: Throwable => None
    }
    Some(Article(
      url,
      driver.findElementByXPath("""//div[@class='title']""").getText,
      driver.findElementById("newsContent").getText
    ))
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







  //   urls match {
  //   case url :: rest_urls => extractUrls(url, pattern) ++ extractAllUrls(rest_urls, pattern)
  //   case _ => Nil
  // }

}
