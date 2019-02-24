package ModLogic;

import basemod.TopPanelItem;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;

public class ConsumeCalculatorButton extends TopPanelItem {

    public static boolean enabled;

    public static Texture IMG = ImageMaster.loadImage("img/ConsumeCalculatorUI/ConsumeCalculatorIcon.png");
    public static final String ID = "cvc_ConsumeValueCalculatorButton";

    private final ValueCalculator calculator;

    public ConsumeCalculatorButton(ValueCalculator calculator) {
        super(IMG, ID);
        this.calculator = calculator;
    }

    @Override
    protected void onClick() {
        if (calculator.isShowing()) {
            calculator.hide();
        } else {
            calculator.show();
        }
    }
}
