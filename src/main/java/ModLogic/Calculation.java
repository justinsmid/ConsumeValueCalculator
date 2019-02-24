/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ModLogic;

/**
 *
 * @author Justin Smid
 */
public class Calculation {
    public enum OrbType {
        Lightning, Frost;
    }
    
    public final int value;
    public final OrbType type;
    public final int amountOfOrbs;
    public final int valuePerOrb;
    public final boolean withConsume;
    
    public Calculation(int value, int amountOfOrbs, int valuePerOrb, OrbType type, boolean withConsume) {
        this.value = value;
        this.amountOfOrbs = amountOfOrbs;
        this.valuePerOrb = valuePerOrb;
        this.type = type;
        this.withConsume = withConsume;
    }
    
    @Override
    public String toString() {
        return value + ", " + type + ", " + withConsume;
    }
}
