import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.monitor.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class MyMonitor {
  private final Monitor monitor;

  public static void main(String[] args) throws IOException, ParseException {
    new MyMonitor();

  }

  public MyMonitor() throws IOException, ParseException {
    MonitorConfiguration monitorConfig = new MonitorConfiguration();
    monitor = new Monitor(new StandardAnalyzer(), new TermFilteredPresearcher(), monitorConfig);

    monitor.register(newMonitorQuery("Gr", "color:green", Collections.singletonMap("customer", "123")));
    monitor.register(newMonitorQuery("Gr_ID", "id:g1", Collections.singletonMap("customer", "124")));
    monitor.register(newMonitorQuery("Bl", "color:blue^2.0", Collections.singletonMap("customer", "123")));
    monitor.register(newMonitorQuery("Bl_title", "sky", Collections.singletonMap("customer", "124")));

    Document greenDoc = newDoc("id", "g1", "color", "green", "title", "Grass");
    log("\nPerforming match for " + greenDoc);
    MatchingQueries<QueryMatch> qm = monitor.match(greenDoc, QueryMatch.SIMPLE_MATCHER);
    log("Got " + qm.getMatchCount() + " matches");
    for (QueryMatch queryMatch : qm.getMatches()) {
      log("Match: " + queryMatch.getQueryId());
    }


    Document blueDoc = newDoc("id", "b1", "color", "blue", "title", "Sky");
    log("\nPerforming match with scores for " + blueDoc);
    MatchingQueries<ScoringMatch> sm = monitor.match(blueDoc, ScoringMatch.DEFAULT_MATCHER);
    log("Got " + sm.getMatchCount() + " matches");
    sm.getMatches().stream().forEach(m -> log("Match: " + m.getQueryId() + " with score " + m.getScore()));

    Document skyDoc = newDoc("id", "s1", "title", "The blue sky is very beautiful and the sky is warm");
    log("\nPerforming match with highlights for " + skyDoc);
    MatchingQueries<HighlightsMatch> hm = monitor.match(skyDoc, HighlightsMatch.MATCHER);
    log("Got " + hm.getMatchCount() + " matches");
    hm.getMatches().forEach(m -> {
      log("Match: " + m.getQueryId() + " with " + m.getHitCount());
      m.getHits("title").forEach(h -> {
        log("  hit: " + h.toString() + " - " + skyDoc.get("title").substring(h.startOffset, h.endOffset));
      });
    });

    log("\nPerforming match with explanation for " + skyDoc);
    MatchingQueries<ExplainingMatch> em = monitor.match(skyDoc, ExplainingMatch.MATCHER);
    log("Got " + em.getMatchCount() + " matches");
    em.getMatches().forEach(m -> {
      log("Match: " + m.getQueryId() + ", explain= " + m.getExplanation());
    });

    monitor.close();
  }

  private Document newDoc(String... kvPairs) {
    Document doc = new Document();
    boolean isKey = true;
    String key = null;
    for (String kv : kvPairs) {
      if (isKey) {
        key = kv;
        isKey = false;
      } else {
        doc.add(new TextField(key, kv, Field.Store.YES));
        isKey = true;
      }
    }
    return doc;
  }

  private MonitorQuery newMonitorQuery(String id, String queryString, Map<String, String> metadata) throws ParseException {
    QueryParser qp = new QueryParser("title", new StandardAnalyzer());
    Query q = qp.parse(queryString);
    log("Registered monitor query " + id);
    return new MonitorQuery(id, q, queryString, metadata);
  }

  private void log(String msg) {
    System.out.println(msg);
  }
}
