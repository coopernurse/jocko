package com.bitmechanic.jocko;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Mar 3, 2010
 */
public class CreditCardPaymentMethod extends PaymentMethod {

    private String cardNumber; // 13 to 16 digits, no dashes
    private String expirationDate; // YYYY-MM
    private String cardCode; // CCV number

    public CreditCardPaymentMethod() {
        super();
    }

    public CreditCardPaymentMethod(String cardNumber, String expirationDate, String cardCode) {
        super();
        this.cardNumber = cardNumber;
        this.expirationDate = expirationDate;
        this.cardCode = cardCode;
    }

    public String getCardCode() {
        return cardCode;
    }

    public void setCardCode(String cardCode) {
        this.cardCode = cardCode;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreditCardPaymentMethod)) return false;
        if (!super.equals(o)) return false;

        CreditCardPaymentMethod that = (CreditCardPaymentMethod) o;

        if (cardCode != null ? !cardCode.equals(that.cardCode) : that.cardCode != null) return false;
        if (cardNumber != null ? !cardNumber.equals(that.cardNumber) : that.cardNumber != null) return false;
        if (expirationDate != null ? !expirationDate.equals(that.expirationDate) : that.expirationDate != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (cardNumber != null ? cardNumber.hashCode() : 0);
        result = 31 * result + (expirationDate != null ? expirationDate.hashCode() : 0);
        result = 31 * result + (cardCode != null ? cardCode.hashCode() : 0);
        return result;
    }
}
