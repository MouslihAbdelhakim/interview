## task #1 : Forex version 1.0.1
this new version of Forex contains the following improvements

* Updated all libraries to their latest versions, to incorporate any bug fixes or security patches.
* Added the recommended Scalac Flags for 2.12 by tpolecat, these flags help me keep code clean and catch avoidable bugs at compile time.
* Fixed to URL path to /exchangeRate to eliminate the risk of breaking existing Forex's clients in case of adding new features.
* Wrote some unit tests for existing code.
* Improved the error reporting by :
    * wrapping the error messages into a json object to keep the API consistent.
    * return the correct http code code (200, 500, 400, 404) so that clients services differentiate easily between valid responses and errors.
    * writing clearer error messages
* Renamed the configuration file to `application.conf` as per PureConfig recommendations, `reference.conf` should be provided by a library not an application.
* Improved the Currency implementation and added all currencies supported by oneForge.
* Added a custom rejection handler.
* Unified the error representations in the code.
* Removed code under package io.circe.generic.extras because it can be found in `circe-generic-extras` version `0.9.3`
* Removed unused and unneeded libraries
* Updated scala version to 2.12.6
* Updated forex version to 1.0.1