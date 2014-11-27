import ratpack.groovy.test.LocalScriptApplicationUnderTest
import ratpack.test.http.TestHttpClient
import ratpack.test.http.TestHttpClients
import ratpack.test.ApplicationUnderTest

class HandlersSpec extends spock.lang.Specification {

    LocalScriptApplicationUnderTest aut = new LocalScriptApplicationUnderTest()
    @Delegate TestHttpClient client = TestHttpClients.testHttpClient(aut)

    def "getting a match with an empty database 404s"() {
        when: "database is clear"
        delete("all")

        and: "a request is made for a non-existent match"
        get("matches/nonexistent")

        then:
        assert response.statusCode == 404,
            "Response was ${response.statusCode}.\n\nBody:\n\n${response.body.getText()}"
    }

}
