package edu.uw.spl.account;

/**Encapsulates a credit card
 * @author slajaunie
 *
 */
public class CreditCardImpl implements edu.uw.ext.framework.account.CreditCard {
    private String accountNumber="default";
    private String expirationDate="default";
    /*Name of the card holder*/
    private String holder="default";
    /*Name of the card issuer*/
    private String issuer="default";
    /*The card type*/
    private String type="default";
    
    /**
     *Instantiates a new Credit Card
     */
    public CreditCardImpl() { }
    
    /**Obtains the account number associated with this credit card
     * @see edu.uw.ext.framework.account.CreditCard#getAccountNumber()
     * @return a String representing the account number associated with this credit card
     */
    @Override
    public String getAccountNumber() {
        return this.accountNumber;
    }

    /**Obtains the expiration date associated with this credit card
     * @see edu.uw.ext.framework.account.CreditCard#getExpirationDate()
     * @return a String representing the expiration date associated with this credit card
     */
    @Override
    public String getExpirationDate() {
        return this.expirationDate;
    }

    /**Obtains the name of the accountholder of this credit card
     * @see edu.uw.ext.framework.account.CreditCard#getHolder()
     * @return a String representing the name of the accountholder associated with this credit card
     */
    @Override
    public String getHolder() {
        return this.holder;
    }

    /**Obtains the name of the issuer of this credit card
     * @see edu.uw.ext.framework.account.CreditCard#getIssuer()
     * @return a String representing the name of the issuer of this credit card
     */
    @Override
    public String getIssuer() {
        return this.issuer;
    }

    /**Obtains the type of this credit card
     * @see edu.uw.ext.framework.account.CreditCard#getType()
     * @return a String representing the type of this credit card
     */
    @Override
    public String getType() {
        return this.type;
    }

    /**Sets the account number associated with this credit card
     * @see edu.uw.ext.framework.account.CreditCard#setAccountNumber(java.lang.String)
     * @param accountNumber a String representing the account number associated with this credit card
     */
    @Override
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    /**Sets the expiration date associated with this credit card
     * @see edu.uw.ext.framework.account.CreditCard#setExpirationDate(java.lang.String)
     * @param expirationDate a String representing the expiration date associated with this credit card
     */
    @Override
    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**Sets the name of the accountholder associated with this credit card
     * @see edu.uw.ext.framework.account.CreditCard#setHolder(java.lang.String)
     * @param holder a String representing the name of the account holder associated with this credit card
     */
    @Override
    public void setHolder(String holder) {
        this.holder = holder;
    }

    /**Sets the name of the issuer associated with this credit card
     * @see edu.uw.ext.framework.account.CreditCard#setIssuer(java.lang.String)
     * @param issuer a String representing the name of the issuer associated with this credit card
     */
    @Override
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**Sets the type of this credit card
     * @see edu.uw.ext.framework.account.CreditCard#setType(java.lang.String)
     * @param type a String representing the type of this credit card
     */
    @Override
    public void setType(String type) {
        this.type = type;
    }

}
