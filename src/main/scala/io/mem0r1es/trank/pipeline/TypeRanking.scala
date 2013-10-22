package io.mem0r1es.trank.pipeline

import java.net.URI

import scala.util.parsing.json.JSON

import org.apache.lucene.index.Term
import org.apache.lucene.search.TermQuery

import com.typesafe.config.Config

import io.mem0r1es.trank.ranking.HierInfo
import io.mem0r1es.trank.ranking.RankingAlgo
import io.mem0r1es.trank.util.IndexUtils
import io.mem0r1es.trank.util.TRankIndexType

object TypeRanking {

  def rankTypes(entityTypes: Map[URI, Set[URI]],
    rankingAlgo: RankingAlgo,
    config: Config): Map[URI, Seq[URI]] = {

    var ranked = Map[URI, Seq[URI]]()

    entityTypes.foreach {
      case (uri, types) =>
        ranked += uri -> rankingAlgo.rank(types.map { t => t -> queryHier(t, config) }.toMap)
    }
    ranked
  }

  private def queryHier(typeURI: URI, config: Config): HierInfo = {
    val searcher = IndexUtils.getIndexSearcher(TRankIndexType.PATH_INDEX, config)

    val query = new TermQuery(new Term("uri", typeURI.toString))
    val docs = searcher.search(query, 1)
    if (docs.scoreDocs.length > 0) {
      val d = searcher.doc(docs.scoreDocs(0).doc)
      val level = d.get("level").toInt
      val path = JSON.parseFull(d.get("path"))
      path.get match {
        case l: List[Any] => new HierInfo(level, l.map(t => new URI(t.toString)))
        case _ => new HierInfo(level, Seq[URI]())
      }
    } else {
      new HierInfo(-1, Seq[URI]())
    }
  }
}
