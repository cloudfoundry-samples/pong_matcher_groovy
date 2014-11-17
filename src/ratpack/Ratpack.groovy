import static ratpack.groovy.Groovy.ratpack
import static java.util.UUID.randomUUID 
import groovy.json.*
import matcher.pong.RedisDriver

db = [
  match_requests: RedisDriver.fromEnv("match_requests"),
  matches: RedisDriver.fromEnv("matches"),
  results: RedisDriver.fromEnv("results")
]

ratpack {
  handlers {
    delete("all") {
      db.match_requests.clear()
      db.matches.clear()
      db.results.clear()
      render "{}"
    }

    prefix("match_requests/:id") {
      handler {
        byMethod {

          put {
            def slurper = new JsonSlurper()
            def payload = slurper.parseText(request.body.text)
            def player_id = payload.player

            new_match_request = [ id: pathTokens.id, requester_id: player_id ]
            db.match_requests << new_match_request

            def open_match_request = first_open_match_request(db, player_id)
            if (open_match_request) {
              record_match(db, open_match_request, new_match_request)
            }
            
            render "{}"
          }

          get {
            def match_request = db.match_requests.find { it.id == pathTokens.id }

            if (match_request) {
              def found_match = find_unplayed_match(db, match_request) ?: [ id: null ]
              def builder = new JsonBuilder()
              builder(id: match_request.id,
                      player: match_request.requester_id,
                      match_id: found_match.id)
              render builder.toString()
            } else {
              clientError(404)
            }
          }
        }
      }
    }

    get("matches/:id") {
      def builder = new JsonBuilder()
      def match = db.matches.find { match ->
        match.id == pathTokens.id
      }
      builder(match)
      render builder.toString()
    }

    post("results") {
      def slurper = new JsonSlurper()
      def result = slurper.parseText(request.body.text)
      db.results << [
        match_id: result.match_id,
        winner: result.winner,
        loser: result.loser
      ]
      response.status(201)
      render "{}"
    }

    assets "public"
  }
}

def record_match(db, open_match_request, new_match_request) {
  db.matches << [
    id: randomUUID().toString(),
    match_request_1_id: open_match_request.id,
    match_request_2_id: new_match_request.id,
    player_1: open_match_request.requester_id,
    player_2: new_match_request.requester_id
  ]
}

def find_unplayed_match(db, match_request) {
  db.matches.find { match ->
    [match.player_1, match.player_2].contains(match_request.requester_id) &&
      !played_match_ids(db).contains(match.id)
  }
}

def played_match_ids(db) {
  db.results.collect { it.match_id }
}

def first_open_match_request(db, player_id) {
  unfulfilled_match_requests(db).find { match_request ->
    inappropriate_opponents = [player_id] + previous_opponents(db, player_id)
    !inappropriate_opponents.contains(match_request.requester_id)
  }
}

def unfulfilled_match_requests(db) {
  db.match_requests.findAll { match_request ->
    !db.matches.any { match ->
      [match.match_request_1_id, match.match_request_2_id].contains(match_request.id)
    }
  }
}

def previous_opponents(db, player_id) {
  results_involving_player(db, player_id).collect { result ->
    result.winner == player_id ? result.loser : result.winner
  }
}

def results_involving_player(db, player_id) {
  db.results.findAll { result ->
    [result.winner, result.loser].contains(player_id)
  }
}
