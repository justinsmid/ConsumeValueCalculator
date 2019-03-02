package ModLogic;

import basemod.BaseMod;
import static basemod.BaseModInit.BUTTON_ENABLE_X;
import static basemod.BaseModInit.BUTTON_ENABLE_Y;
import static basemod.BaseModInit.BUTTON_LABEL_X;
import static basemod.BaseModInit.BUTTON_LABEL_Y;
import static basemod.BaseModInit.BUTTON_X;
import static basemod.BaseModInit.BUTTON_Y;
import basemod.ModButton;
import basemod.ModLabel;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PostRenderSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import java.io.IOException;
import java.util.Properties;

@SpireInitializer
public class ConsumeValueCalculator implements PostInitializeSubscriber, PostRenderSubscriber {
	public static final String CONFIG_FILE = "cvc-config";
	public static final String MOD_NAME = "Consume Value Calculator";
	public static final String AUTHOR = "Justin Smid";
	public static final String DESCRIPTION = "Calculates the value of using consume";
	private static SpireConfig config;

	private InputProcessor oldInputProcessor;

	private ValueCalculator calculator;
	private static ConsumeCalculatorButton calculatorButton;

	public ConsumeValueCalculator() {
		BaseMod.subscribe(this);
		calculator = new ValueCalculator();
	}

	public static void initialize() {
		try {
			new ConsumeValueCalculator();
			config = makeConfig();
			setProperties();
		} catch (Exception ex) {
			System.out.println("Caught exception during initialization");
			ex.printStackTrace();
		}

	}

	private static SpireConfig makeConfig() {
		Properties defaultProperties = new Properties();
		defaultProperties.setProperty("toggle-key", "H");
		defaultProperties.setProperty("top-button-enabled", "true");

		try {
			SpireConfig retConfig = new SpireConfig(MOD_NAME, CONFIG_FILE, defaultProperties);
			return retConfig;
		} catch (IOException e) {
			System.out.println("Caught exception when trying to make configurations");
			return null;
		}
	}

	private static void setProperties() {
		// if config can't be loaded leave things at defaults
		if (config == null) {
			return;
		}

		String toggleKey = getString("toggle-key");
		if (toggleKey != null) {
			ValueCalculator.toggleKey = Keys.valueOf(toggleKey);
		}

		Boolean topButtonEnabled = getBoolean("top-button-enabled");
		if (topButtonEnabled != null) {
			ConsumeCalculatorButton.enabled = topButtonEnabled;
		}
	}

	@Override
	public void receivePostInitialize() {
		// I don't know why this is needed, but the IMG property is null if we don't set it here..
		ConsumeCalculatorButton.IMG = ImageMaster.loadImage("img/ConsumeCalculatorUI/ConsumeCalculatorIcon.png");
		calculatorButton = new ConsumeCalculatorButton(calculator);

		ModPanel modPanel = new ModPanel();

		ModLabeledToggleButton enableTopBarButton = new ModLabeledToggleButton("Enable button in top bar",
				BUTTON_ENABLE_X, BUTTON_ENABLE_Y, Color.WHITE, FontHelper.charDescFont,
				ConsumeCalculatorButton.enabled, modPanel, (label) -> {
				}, (button) -> {
					ConsumeCalculatorButton.enabled = button.enabled;
					setBoolean("top-button-enabled", button.enabled);
				});
		modPanel.addUIElement(enableTopBarButton);

		ModLabel buttonLabel = new ModLabel("Hotkey", BUTTON_LABEL_X, BUTTON_LABEL_Y, modPanel, (me) -> {
			if (me.parent.waitingOnEvent) {
				me.text = "Press key";
			} else {
				me.text = "Change calculator hotkey (" + Keys.toString(ValueCalculator.toggleKey) + ")";
			}
		});
		modPanel.addUIElement(buttonLabel);

		ModButton consoleKeyButton = new ModButton(BUTTON_X, BUTTON_Y, modPanel, (me) -> {
			me.parent.waitingOnEvent = true;
			oldInputProcessor = Gdx.input.getInputProcessor();
			Gdx.input.setInputProcessor(new InputAdapter() {
				@Override
				public boolean keyUp(int keycode) {
					ValueCalculator.toggleKey = keycode;
					setString("toggle-key", Keys.toString(keycode));
					me.parent.waitingOnEvent = false;
					Gdx.input.setInputProcessor(oldInputProcessor);
					return true;
				}
			});
		});
		modPanel.addUIElement(consoleKeyButton);

		Texture badgeTexture = ImageMaster.loadImage("img/ConsumeCalculatorUI/ConsumeCalculatorIcon_small.png");
		BaseMod.registerModBadge(badgeTexture, MOD_NAME, AUTHOR, DESCRIPTION, modPanel);

		if (ConsumeCalculatorButton.enabled) {
			BaseMod.addTopPanelItem(calculatorButton);
		}
	}

	private static String getString(String key) {
		return config.getString(key);
	}

	private static Boolean getBoolean(String key) {
		return config.getBool(key);
	}

	public static void setBoolean(String key, Boolean value) {
		config.setBool(key, value);
		try {
			config.save();
			if (key.equals("top-button-enabled")) {
				if (value) {
					BaseMod.addTopPanelItem(calculatorButton);
				} else if (!value) {
					BaseMod.removeTopPanelItem(calculatorButton);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void setString(String key, String value) {
		config.setString(key, value);
		try {
			config.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void receivePostRender(SpriteBatch sb) {
		if (calculator.isShowing() && calculator.canShow()) {
			calculator.render(sb);
		}
	}
}
