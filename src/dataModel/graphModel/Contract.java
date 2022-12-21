/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataModel.graphModel;

import java.util.Objects;

/**
 * Ethereum Contract
 * @author andreads
 */
public class Contract extends NodeEth {

    public Contract(String adr) {
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
        Contract c2 = (Contract) o;
        // field comparison
        return Objects.equals(address, c2.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
    
    @Override
    public String toString(){
        return "Contract";
    }
}
