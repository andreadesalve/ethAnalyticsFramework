/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataModel.graphModel;

import java.util.Objects;

/**
 * Ethereum EOA
 * @author andreads
 */
public class Account extends NodeEth {
    
    public Account(String adr){
        super(adr);
    }
      @Override
    public boolean equals(Object o) {
        // self check
        if (this == o) {
            return true;
        }
        // null check
        if (o == null) {
            return false;
        }
        // type check and cast
        if (getClass() != o.getClass()) {
            return false;
        }
        Account c2 = (Account) o;
        // field comparison
        return Objects.equals(address, c2.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
    
     @Override
    public String toString(){
        return "EOA";
    }
}
