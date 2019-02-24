/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ModLogic;

import basemod.BaseMod;
import basemod.ClickableUIElement;
import com.badlogic.gdx.graphics.Color;
import basemod.interfaces.PostUpdateSubscriber;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;

/**
 *
 * @author Justin Smid
 */
public class UpgradedConsumeToggle extends ClickableUIElement implements PostUpdateSubscriber {

    private final Texture CHECKMARK;
    BitmapFont font;
    
    private boolean toggled = false;
    
    public UpgradedConsumeToggle(Texture baseButtonTexture, Texture checkmarkTexutre, int x, int y, int width, int height) {
        super(baseButtonTexture, x, y, width, height);
        this.CHECKMARK = checkmarkTexutre;
        this.font = FontHelper.tipHeaderFont;
        BaseMod.subscribe(this);
    }
    
    public void render(SpriteBatch sb, boolean drawCheckmark) {
        render(sb);
        FontHelper.renderFont(sb, font, "Upgraded Consume", (x + 30), (y + hb_h), (toggled) ? Color.GREEN : Color.WHITE);
        if(drawCheckmark) {
            sb.draw(this.CHECKMARK, (x + 3), (y + 5), 32 * Settings.scale, 32 * Settings.scale);
        }
    }

    public boolean isToggled() {
        return toggled;
    }

    @Override
    public void onClick() {
        toggled = !toggled;
        ValueCalculator.consumeValue = (toggled) ? 3 : 2;
    }
    
    @Override
    public void receivePostUpdate() {
        update();
    }

    @Override
    protected void onHover() {
        
    }

    @Override
    protected void onUnhover() {
        
    }
}
