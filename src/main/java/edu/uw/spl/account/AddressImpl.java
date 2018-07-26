package edu.uw.spl.account;

/**Encapsulates an address associated with an Account
 * @author slajaunie
 *
 */
public class AddressImpl implements edu.uw.ext.framework.account.Address {
    
    private String city="default";
    private String state="default";
    private String streetAddress="default";
    private String zipCode="default";
    
    /**
     *No argument constructor 
     */
    public AddressImpl() { }
    
    /** Obtains the city associated with this address
     * @see edu.uw.ext.framework.account.Address#getCity()
     * @return a String that is the city associated with this address
     */
    @Override
    public String getCity() {
        return this.city;
    }

    /** Obtains the state associated with this address
     * @see edu.uw.ext.framework.account.Address#getState()
     * @return a String that is the state associated with this address
     */
    @Override
    public String getState() {
        return this.state;
    }

    /**Obtains the street address associated with this address
     * @see edu.uw.ext.framework.account.Address#getStreetAddress()
     * @return a String that is the street address associated with this address
     */
    @Override
    public String getStreetAddress() {
        return this.streetAddress;
    }

    /**Obtains the zip code associated with this address
     * @see edu.uw.ext.framework.account.Address#getZipCode()
     * @return a String that is the zip code associated with this address
     */
    @Override
    public String getZipCode() {
        return this.zipCode;
    }

    /**Sets the city to be associated with this Address
     * @see edu.uw.ext.framework.account.Address#setCity(java.lang.String)
     * @param city a String that is to be the city associated with this address
     */
    @Override
    public void setCity(String city) {
        this.city = city;
    }

    /**Sets the state to be associated with this Address
     * @see edu.uw.ext.framework.account.Address#setState(java.lang.String)
     * @param state a String that is to be the state associated with this address
     */
    @Override
    public void setState(String state) {
        this.state = state;
    }

    /** Sets the street address to be associated with this Address
     * @see edu.uw.ext.framework.account.Address#setStreetAddress(java.lang.String)
     * @param streetAddress a String that is to be the street address associated with this address
     */
    @Override
    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    /**Sets the zip code to be associated with this Address
     * @see edu.uw.ext.framework.account.Address#setZipCode(java.lang.String)
     * @param zipCode a String that is to be the zip code associated with this address
     */
    @Override
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

}
