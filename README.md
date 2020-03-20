# ![RealWorld Example App](logo.png)

> ### Hoplon and shadow-cljs codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.


### [Demo](https://hoplon-realworld.netlify.com)&nbsp;&nbsp;&nbsp;&nbsp;[RealWorld](https://github.com/gothinkster/realworld)


This codebase was created to demonstrate a fully fledged fullstack application built with Hoplon including CRUD operations, authentication, routing, pagination, and more.

We've gone to great lengths to adhere to the Hoplon community styleguides & best practices.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.


## Setup And Run

#### Copy repository
```shell
git clone https://github.com/rlander/conduit.git && cd conduit
```

#### Run dev server
```shell
 npx shadow-cljs watch app
```

#### Compile an optimized version

```shell
 npx shadow-cljs release app
```