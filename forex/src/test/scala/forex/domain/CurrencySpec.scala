package forex.domain

import forex.domain.Currency._
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import cats.syntax.show._

class CurrencySpec extends FlatSpec with Matchers with TableDrivenPropertyChecks {

  private val currencies = Table(
    "currencies",
    AUD,
    CAD,
    CHF,
    EUR,
    GBP,
    NZD,
    JPY,
    SGD,
    USD,
  )

  "Currency.fromString" should "return the currency object when passed currency's string representation" in forAll(currencies) { currency: Currency ⇒
    Currency.fromString(currency.show) shouldBe Right(currency)
  }

  it should "support currency's lowercase string representation" in forAll(currencies) { currency: Currency ⇒
    Currency.fromString(currency.show.toLowerCase) shouldBe Right(currency)
  }

  it should "return an instant of Left with an error message if currency code is not supported" in {
    Currency.fromString("silverPennies") shouldBe a[Left[_,_]]
  }

}
