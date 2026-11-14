package tweets

import java.util
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.{ApplicationRunner, SpringApplication}
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.{GetMapping, RestController}
import jakarta.persistence.{Column, Entity, GeneratedValue, GenerationType, Id}

import scala.beans.BeanProperty
import scala.collection.JavaConverters

@SpringBootApplication
class Application {

  @Bean
  def init(tr: TweetRepository): ApplicationRunner = _ => {
    val viktor = "viktorklang"
    val jonas = "jboner"
    val josh = "starbuxman"
    val tweets = List(
      Tweet("Woot, Konrad will be talking about #Enterprise #Integration done right! #akka #alpakka", viktor),
      Tweet(
        s"""#scala implicits can easily be used to model Capabilities, but can they encode Obligations easily?
           |* Easy as in: ergonomically.
         """.stripMargin, viktor),
      Tweet("This is so cool! #akka", viktor),
      Tweet("Cross Data Center replication of Event Sourced #Akka Actors is soon available (using #CRDTs, and more).", jonas),
      Tweet("a reminder: @SpringBoot lets you pair-program with the #Spring team.", josh),
      Tweet("whatever your next #platform is, don't build it yourself. \n\nEven companies with the $$ and motivation to do it fail. a LOT.", josh)
    )
    tr.deleteAll()
    tr.saveAll(JavaConverters.seqAsJavaList(tweets))
    tr.findAll().forEach((t: Tweet) => println(
      s"""=====================================================
         |@${t.getAuthor.handle} ${t.getHashtags}
         |${t.getText}
       """.stripMargin
    ))
  }
}

@Entity
class Tweet {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: java.lang.Long = _

  @Column(length = 4096)
  var text: String = _

  var authorHandle: String = _

  def getText: String = text
  def setText(value: String): Unit = { text = value }

  def getAuthorHandle: String = authorHandle
  def setAuthorHandle(value: String): Unit = { authorHandle = value }

  def getAuthor: Author = Author(authorHandle)

  def getHashtags: util.Set[HashTag] = JavaConverters.setAsJavaSet(
    Option(text).getOrElse("").split(" ").collect {
      case t if t.startsWith("#") => HashTag(t.replaceAll("[^#\\w]", "").toLowerCase())
    }.toSet
  )
}

object Tweet {
  def apply(text: String, authorHandle: String): Tweet = {
    val t = new Tweet
    t.setText(text)
    t.setAuthorHandle(authorHandle)
    t
  }
}

case class Author(@BeanProperty handle: String)

case class HashTag(@BeanProperty tag: String)

trait TweetRepository extends JpaRepository[Tweet, java.lang.Long]

@Service
class TweetService(tr: TweetRepository) {

  def tweets(): util.List[Tweet] = tr.findAll()

  def hashtags(): util.Set[HashTag] = JavaConverters.setAsJavaSet(
    JavaConverters.asScalaBuffer(tr.findAll())
      .flatMap(t => JavaConverters.asScalaSet(t.getHashtags))
      .toSet
  )
}

@RestController
class TweetRestController(ts: TweetService) {

  @GetMapping(Array("/hashtags/unique"))
  def hashtags(): util.Set[HashTag] = ts.hashtags()

  @GetMapping(Array("/tweets"))
  def tweets(): util.List[Tweet] = ts.tweets()

  @GetMapping(Array("/ping"))
  def ping(): String = "hello world"
}

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
