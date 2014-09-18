import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack
import static java.util.UUID.randomUUID 
import groovy.json.JsonSlurper

db = [
  "match_requests": [],
  "matches": [],
  "results": []
]

def played_match_ids(db) {
  db["results"].collect { it["match_id"] }
}

def find_unplayed_match(db, match_request) {
  db["matches"].
    findAll { !played_match_ids(db).contains(it["id"]) }.
    find {
      it["player_1"] == match_request["requester_id"] ||
        it["player_2"] == match_request["requester_id"] 
    }
}

def results_involving_player(db, player_id) {
  db["results"].findAll {
    [it["winner"], it["loser"]].contains(player_id)
  }
}

def previous_opponents(db, player_id) {
  results_involving_player(db, player_id).collect {
    it["winner"] == player_id ? it["loser"] : it["winner"]
  }
}

def unfulfilled_match_requests(db) {
  db["match_requests"].findAll { match_request ->
    !db["matches"].any { match ->
      match["match_request_1_id"] == match_request["id"] ||
        match["match_request_2_id"] == match_request["id"]
    }
  }
}

def first_open_match_request(db, player_id) {
  unfulfilled_match_requests(db).find {
    inappropriate_opponents = [player_id] + previous_opponents(db, player_id)
    !inappropriate_opponents.contains(it["requester_id"])
  }
}

def record_match(db, open_match_request, new_match_request) {
  db["matches"] << [
    id: randomUUID(),
    match_request_1_id: open_match_request["id"],
    match_request_2_id: new_match_request["id"],
    player_1: open_match_request["requester_id"],
    player_2: new_match_request["requester_id"]
  ]
}

ratpack {
  handlers {
    delete("all") {
      db["match_requests"].clear()
      db["matches"].clear()
      db["results"].clear()
      response.send "{}"
    }

    prefix("match_requests/:id") {
      handler {
        byMethod {

          put {
            def slurper = new JsonSlurper()
            def payload = slurper.parseText(request.body.text)
            def player_id = payload["player"]

            new_match_request = [ id: pathTokens.id, requester_id: player_id ]
            db["match_requests"] << new_match_request

            def open_match_request = first_open_match_request(db, player_id)
            if (open_match_request) {
              record_match(db, open_match_request, new_match_request)
            }
            
            render "{}"
          }

          get {
            def match_request = db["match_requests"].find {
              it["id"] == pathTokens.id
            }
            def found_match = find_unplayed_match(db, match_request)

            if (found_match) {
              def builder = new groovy.json.JsonBuilder()
                builder(match_id: found_match["id"])
                response.send(builder.toString())
            } else {
              clientError(404)
            }
          }

        }
      }
    }

    post("results") {
      def slurper = new JsonSlurper()
      def result = slurper.parseText(request.body.text)
      db["results"] << [
        match_id: result["match_id"],
        winner: result["winner"],
        loser: result["loser"]
      ]
      response.status(201)
      response.send("{}")
    }

    assets "public"
  }
}
