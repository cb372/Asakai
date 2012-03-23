package controllers


import io.Source
import play.api.mvc._
import play.api.Play
import Play.current
import java.io.{File, FilenameFilter}
import play.api.templates.HtmlFormat
import org.pegdown.PegDownProcessor

object Slide extends Controller {

  def slide(i: Int) = Action {
    val mdFile = Play.getExistingFile("slides/%d.md".format(i))
    mdFile match {
      case Some(mdFile) => {
        val (title, markdown) = withSource(mdFile) {
          src => {
            val title = src.getLines().take(1).toSeq.head
            val body = src.getLines().drop(2).toSeq.mkString("", "\n", "")
            (title, body)
          }
        }
        val html = new PegDownProcessor().markdownToHtml(markdown)
        val content = HtmlFormat.raw(html)
        val id = extractSlideId(mdFile.getName)
        val (prevId, nextId) = getPrevAndNextSlideIds(id)

        Ok(views.html.slide(id, title, content, prevId, nextId))
      }
      case None => NotFound
    }
  }

  def contents = Action {
    val slidesDir = Play.getExistingFile("slides")

    val idsAndTitles = slidesDir.map(dir => {
      // mdファイルのリストを取る
      val mdFiles = dir.listFiles(new FilenameFilter {
        def accept(dir: File, name: String) = name.endsWith(".md")
      })

      // 各スライドのIDとタイトルを取得
      val idsAndTitles: Seq[(Int, String)] = mdFiles.flatMap(file => {
        withSource(file) {
          src =>
            src.getLines().take(1).toSeq.headOption
        }.map(title => (extractSlideId(file.getName), title))
      })

      // IDで並び替える
      idsAndTitles.sortBy(_._1)
    }).getOrElse(List[(Int, String)]())

    Ok(views.html.contents(idsAndTitles))
  }

  private def getPrevAndNextSlideIds(currId: Int) = {
    val prev = Play.getExistingFile("slides/%d.md".format(currId - 1)).map(_ => currId - 1)
    val next = Play.getExistingFile("slides/%d.md".format(currId + 1)).map(_ => currId + 1)
    (prev, next)
  }

  private def extractSlideId(filename: String) = filename.takeWhile(_ != '.').toInt

  private def withSource[A](file: File)(f: Source => A): A = {
    val src = Source.fromFile(file, "UTF-8")
    try {
      f(src)
    } finally {
      src.close()
    }
  }

}