/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ModLogic;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 *
 * @author Justin Smid
 */
public class CalculationsPanel {

    private Texture panel;

    private boolean loaded = false;

    public CalculationsPanel() {
    }

    public void render(SpriteBatch sb) {
        if (!loaded) {
            panel = ImageMaster.loadImage("img/ConsumeCalculatorUI/panel.png");
            loaded = true;
        }
        sb.draw(panel, 1450 * Settings.scale, 595 * Settings.scale, 450 * Settings.scale, 360 * Settings.scale);
    }
}
