## Postmortem
I learned so much from this exercise and it nudged me toward trying the FP side of scala in the future, 
so if i would redo this exercise i would go with the TypeLevel stack. 

In summary the things i got out with from this exercise are:
* I finally understand what `Separating program description from evaluation` means
* I used a bunch of libraries i was not familiar with like graftter, circe and akka-http 
* I embraced implicit parameters a little more
* Scala compilers error messages are just awful when it comes to implicits
* creating a package object containing aliases of types from other packages can help decouple packages

Soo many things can be done to improve on my solution, a few ideas are:
* configure akka-http to use https instead of http
* better error management
* write documentation on how to use it and deploy it
* write more automated tests
* improve logging
* support loading configuration from a file outside of the compiled jar

## Run Forex

To run forex make sure you have that you have [Java 8](https://www.oracle.com/technetwork/java/javase/overview/java8-2100321.html) and [sbt](https://www.scala-sbt.org/) installed.

You can grab the repo using git

```shell
git clone https://github.com/MouslihAbdelhakim/interview.git
cd interview
git checkout solution
cd forex
```

Use sbt to run tests and forex
```shell
sbt
test
run
```

When the application starts you can access it from your terminal

```shell
currencies='EUR 
USD 
JPY 
CHF 
AUD 
CAD 
NZD 
GBP 
SEK 
NOK 
MXN 
TRY 
ZAR 
CNH 
XAU 
XAG 
SGD 
RUB 
HKD 
DKK 
PLN 
BTC 
ETH 
LTC 
XRP 
DSH 
BCH'
for from in $currencies; do
    for to in $currencies; do
        curl "http://127.0.0.1:8888/exchangeRate?from=${from}&to=${to}"
        echo ""
    done
done
```

## Solution

The next two sections discuss the thoughts behind some of the decision that went into the solution.

The title of each section is the title of the related commit.

### task 1 : Forex version 1.0.1
This new version of Forex contains the following improvements

* Updated all libraries to their latest versions, to incorporate any bug fixes or security patches.
* Added the recommended Scalac Flags for 2.12 by tpolecat, these flags help me keep code clean and catch avoidable bugs at compile time.
* Fixed to URL path to /exchangeRate to eliminate the risk of breaking existing Forex's clients in case of adding new features.
* Wrote some unit tests for existing code.
* Improved the error reporting by :
    * wrapping the error messages into a json object to keep the API consistent.
    * return the correct http code code (200, 500, 400, 404) so that clients services differentiate easily between valid responses and errors.
    * writing clearer error messages
* Renamed the configuration file to `application.conf` as per PureConfig recommendations, `reference.conf` should be provided by a library not an application.
* Improved the Currency implementation and added all currencies supported by 1forge.
* Added a custom rejection handler.
* Unified the error representations in the code.
* Removed code under package io.circe.generic.extras because it can be found in `circe-generic-extras` version `0.9.3`
* Removed unused and unneeded libraries
* Updated scala version to 2.12.6
* Updated forex version to 1.0.1


### task 2 : Forex version 1.1.0, connecting Forex to the 1forge Api.
This new version of Forex contains the following improvements:
* Serving real exchange rates retrieved from 1forge

Forex refreshes the exchange rates each 5 minutes, and as long as the market is open you get the latest exchange rate.

#### calucating echange rates from a currency pairs
A user of Forex should be able to ask for an exchange rate from a source currency to a target currency, and Forex should use the 1forge api to retrieve a currency pair and use that to calculate the exchange rate that will sent to the user.

[An exchange rate](https://en.wikipedia.org/wiki/Exchange_rate) is the rate at which the source currency will be exchanged for the target currency, meaning

`exchange rate * source currency = target currency`

[A quotation of a currency pair](https://en.wikipedia.org/wiki/Currency_pair) is the relative value of a currency unit against the unit of another currency in the foreign exchange market. The currency that is used as the reference is called the counter currency, and the currency that is quoted in relation is called the base currency, meaning

`base currency = quotation * counter currency`

from this we can say that :

 * the target currency is the base currency
 * the source currency is the counter currency
 * the exchange rate is the quotation

#### choosing a quotation/exchange rate.
1forge's API provide three quotation for each currency pair :

* a bid: the rate at which buyers offer to buy currencies from sellers.
* an ask : the rate at which sellers offer currencies to buyers.
* a price : the average of of the bid and the ask.

I assume that inside Paidy, the exchange rate provided by Forex will be used to convert a transaction amount from a customer's currency into a merchant's currency, after some google i found that [OANDA](https://www.oanda.com/currency/help/how-to-read-currency-conversion-results) uses the bid price for their currency conversion applications,as it more accurately mimics the rate that someone would be charged if you they were exchanging money.

#### making Forex serve 10k requests per day.
Since one of the constraints of this excericise is to use the free tier of 1forge API, which supports only 1k requests per day, it becomes evident that a caching solution needs to be implemented. If we took into consideration the requirement that the exchange rates served to Forex's users should not be older that 5 minutes than the the caching solution should be a time based cache solution where Forex retrieves all supported currency pairs quotation at least once each 5 minutes, this is possible because 1forge lets us include any number of currency pairs in our request.

So which key/value pair should Forex cache ? 

The intuitive and straight forward solution is to use the currency pair as a key and the quotation as the value, this solution is prone to a problem known as [cache stampede or thundering herd](https://en.wikipedia.org/wiki/Cache_stampede) because of two factors :

* our server is implemented using Akka-Http wich means that each request will be isolated and processed conccurently
* the cache is refreshed with a single read from the 1forge api

So when the cache misses under heavy loads the straight forward solution will make multiple requests to the 1forge API, reducing our remaining requests quota rapidly and Forex will not be able to serve anymore requests.

To avoid this we should cache a single data structure containing the call to the 1forge api, this way we can continue to serve requests by mapping the call to the appropriate response. a second thing to take into consideration is that the call should be cached only if it was successful, because we do not want to Forex to be unavailable for 5 minutes of the call fails, so what ever our solution is it should retry the call if it fails and then cache it when it succeeds.

The simplest solution would be to use an Akka actor, the actor can safely store the call to the 1forge Api and map it to a the right rate for each request, and if the call fails it would be easy to implement scheduled retries.

### task 3 : cleaned up the code added more unit tests and some integration tests

I revisited my solution to prepare for the follow up interview, found some small hacks that bugged me and that needed clean up.

I also added some integration and unit tests.