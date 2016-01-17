package org.bitbucket.eunjeon.seunjeon.elasticsearch

import org.bitbucket.eunjeon.seunjeon.{LNode, MorphemeType, Pos, Analyzer}
import org.bitbucket.eunjeon.seunjeon.Pos.Pos
import scala.collection.JavaConverters._


object TokenBuilder {
  val INDEX_POSES = Set[Pos](
    Pos.N,  // 체언
    Pos.SL, // 외국어
    Pos.SH, // 한자
    Pos.SN, // 숫자
    Pos.XR, // 어근
    Pos.V, // 용언
    Pos.UNK)

  lazy val INDEX_POSES_JAVA = INDEX_POSES.map(_.toString).toArray

  def convertPos(poses: Array[String]): Set[Pos] = {
    poses.map(Pos.withName).toSet
  }

  def setUserDict(userWords:Array[String]): Unit = {
    Analyzer.setUserDict(userWords.toSeq.iterator)
  }

  def setUserDict(userWords:java.util.Iterator[String]): Unit = {
    Analyzer.setUserDict(userWords)
  }
}


class TokenBuilder(deCompound:Boolean, deInflect:Boolean, indexEojeol:Boolean, indexPoses:Set[Pos]) {
  def this() {
    this(true, true, true, TokenBuilder.INDEX_POSES)
  }

  def tokenize(document:String): java.util.List[LuceneToken] = {
    val analyzed = Analyzer.parseEojeol(document)
    val deCompounded = if (this.deCompound) analyzed.map(_.deCompound()) else analyzed
    val deInflected = if (this.deInflect) deCompounded.map(_.deInflect()) else deCompounded
    deInflected.flatMap { eojeol =>
      val nodes = eojeol.nodes.filter(isIndexNode).map(LuceneToken(_))

      if (this.indexEojeol) {
        if (eojeol.nodes.length > 1 && nodes.nonEmpty) {
          val eojeolNode = LuceneToken(s"${eojeol.surface}/EOJ", 0, nodes.length, eojeol.startPos, eojeol.endPos, "EOJ")
          nodes.head +: eojeolNode +: nodes.tail
        } else nodes
      } else nodes
    }.asJava
  }

  private def isIndexNode(node:LNode): Boolean = {
    node.morpheme.mType == MorphemeType.COMPOUND ||
      node.morpheme.mType == MorphemeType.INFLECT ||
      (node.morpheme.mType == MorphemeType.COMMON && indexPoses.contains(node.morpheme.poses.head))
  }

}