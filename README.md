# About

This is a [Todo-Backend](https://www.todobackend.com/) implementation in Scala using [Tapir](https://github.com/softwaremill/tapir) its [Http4s](https://github.com/http4s/http4s) interpreter. It is meant as an example project for those wanting to see Tapir **in-action**.

# Architecture

## [tapir.todomvc.Endpoints](src/main/scala/tapir/todomvc/Endpoints.scala)

This class defines a declarative model of the Todo-MVC API using Tapir. Furthermore, it implements a `PlainCodec[UUID]` allowing us to decode `UUID`s directly from path parameters. These endpoints are meant to be hooked up with logic before we can materialize a server

The endpoints are also transformed into `OpenAPI` models which are then merged into a single model and rendered as `.yml`.
This `.yml` file can then be delivered with whatever underlying web library is chosen.

## [tapir.todomvc.Implementation[F[_]]](src/main/scala/tapir/todomvc/Implementation.scala)

This class contains all the implementations of the different API-endpoints. Furthermore, it maps the endpoints to implementations, exposing a single method: `def routes: HttpRoutes[F]`.
These routes are the final model for building and running the server with endpoints and logic mapped together. Note that the endpoints will be matched in the order they appear in the `NonEmptyList`.

We use `combineK` to turn all the implemented routes into a single route. This is possible because Cats implements a `SemigroupK` instance for `Kleisli`, and because `type HttpRoutes[F[_]] = Kleisli[OptionT[F, ?], Request[F], Response[F]]`. The combination works as one would expect -- we try the first route, if the materialized `OptionT` value is `None`, we try the next route.
 
**NOTE:** To me, this is one of the big wins with Tapir -- you don't lose composability, since interpreting the endpoints simply nets you a composable piece of, e.g., `HttpRoutes[F[_]]` that you can continue to compose. 

Finally, this class also exposes `/openapi.yml` as well as a `/docs` which redirects to the index of some `swagger-ui` resources (which will be provided through web jars, see [Main](src/main/scala/tapir/todomvc/Main.scala)).

## [tapir.todomvc.Main](src/main/scala/tapir/todomvc/Main.scala)

This is an `IOApp` using [`cats-effect`](https://github.com/typelevel/cats-effect) that configures and runs the server using the other classes. Currently, it simply binds to `127.0.0.1:8080`. It also exposes static resources from the `swagger-ui` webjar using `http4s`' built in support.

# Running the app

Place yourself in the root folder and issue `sbt run`. Once the server is up, you can go [to this page](http://www.todobackend.com/specs/index.html?http://127.0.0.1:8080/todo/) and run the test suite against your running app. This is possible because `tapir.todomvc.Main` sets up the neccessary `CORS` configuration.

# Viewing the docs

After running the app, you can visit the documentation by going to http://127.0.0.1:8080/docs. 
To view the raw `OpenAPI` specification, go to http://127.0.0.1:8080/openapi.yml.