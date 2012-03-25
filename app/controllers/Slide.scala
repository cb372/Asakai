package controllers


import models._
import io.Source
import play.api.mvc._
import play.api.Play
import Play.current
import java.io.{File, FilenameFilter}
import play.api.templates.HtmlFormat
import org.pegdown.{Extensions, PegDownProcessor}

object Slide extends Controller {

  // 指定のIDに街頭するスライドをmdファイルから読み込む
  def slide(id: Int) = Action {
    val mdFile = Play.getExistingFile("slides/%d.md".format(id))
    mdFile match {
      case Some(mdFile) => {
        // スライド内容をmdファイルから読み込む
        val (title, markdown) = withSource(mdFile) {
          src => {
            val title = src.getLines().take(1).toSeq.head
            val body = src.getLines().drop(2).toSeq.mkString("", "\n", "")
            (title, body)
          }
        }
        // md -> HTML変換
        val html = new PegDownProcessor(Extensions.AUTOLINKS).markdownToHtml(markdown)

        val content = HtmlFormat.raw(html)
        val (prevId, nextId) = getPrevAndNextSlideIds(id)

        Ok(views.html.slide(title, content, prevId, nextId))
      }
      // mdファイルが見つからない -> 404
      case None => NotFound
    }
  }

  // 目次を作成する
  def contents = Action {
    val slidesDir = Play.getExistingFile("slides")

    val slideInfos = slidesDir.map(dir => {
      // mdファイルのリストを取る
      val mdFiles = dir.listFiles(new FilenameFilter {
        def accept(dir: File, name: String) = name.endsWith(".md")
      })

      // 各スライドのIDとタイトルを取得
      val slideInfos: Seq[SlideInfo] = mdFiles.flatMap(file => {
        withSource(file) {
          src =>
            src.getLines().take(1).toSeq.headOption
        }.map(title => (SlideInfo(extractSlideId(file.getName), title)))
      })

      // IDで並び替える
      slideInfos.sortBy(_.id)
    }).getOrElse(List[SlideInfo]())

    Ok(views.html.contents(slideInfos))
  }

  private def getPrevAndNextSlideIds(currId: Int): (Option[Int], Option[Int]) = {
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