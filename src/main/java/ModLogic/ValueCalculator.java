/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ModLogic;

import ModLogic.Calculation.OrbType;
import basemod.BaseMod;
import basemod.interfaces.OnStartBattleSubscriber;
import basemod.interfaces.PostBattleSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.Frost;
import com.megacrit.cardcrawl.orbs.Lightning;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import java.util.ArrayList;
import com.megacrit.cardcrawl.powers.AbstractPower;

/**
 *
 * @author Justin Smid
 */
public class ValueCalculator implements OnStartBattleSubscriber, PostBattleSubscriber, PostUpdateSubscriber {

	public static int toggleKey = Input.Keys.H;

	private UpgradedConsumeToggle upgradeToggle;
	private final CalculationsPanel panel;

	BitmapFont font;

	private boolean canShow = false;
	private boolean showing = false;

	private int currentLightningOrbCount;
	private int currentFrostOrbCount;
	private AbstractOrb firstOrb;
	private AbstractOrb lastOrb;
	private boolean hasLightning = false;
	private boolean hasFrost = false;
	public static int consumeValue = 2;
	private final ArrayList<Calculation> calculations = new ArrayList<>();

	// Used to determine whether consuming is better than not consuming
	private boolean greaterDamage = false;
	private boolean greaterBlock = false;
	private boolean equalDamage = false;
	private boolean equalBlock = false;

	// Used to determine whether we need to recalculate the values
	private boolean needToCalculate;
	private int previousTotalOrbCount;
	private int previousFilledOrbCount;
	private int previousFocus = 0;
	private boolean previousUpgraded = false; // was upgradeToggle toggled?
	private int previousAmountOfLoops = 0;
	private int currentAmountOfLoops = 0;

	private boolean loaded = false;

	public ValueCalculator() {
		BaseMod.subscribe(this);
		panel = new CalculationsPanel();
	}

	public void render(SpriteBatch sb) {
		try {
			needToCalculate = checkIfChangesOccurred();
			if (needToCalculate) {
				calculate();
			}
			if (!loaded) {
				this.font = FontHelper.tipHeaderFont;
				loaded = true;
			}
			panel.render(sb);
			drawCalculations(sb);
			upgradeToggle.render(sb, upgradeToggle.isToggled());
			// Make sure the cursor renders after the panel so the cursor doesn't get lost behind it
			CardCrawlGame.cursor.render(sb);
		} catch (Exception ex) {
			hide();
			System.out.println("caught following exception while rendering consume value calculator: ");
			ex.printStackTrace();
		}
	}

	private void calculate() {
		// Reset variables that need resetting
		calculations.clear();
		greaterDamage = false;
		greaterBlock = false;
		equalDamage = false;
		equalBlock = false;

		AbstractPlayer player = AbstractDungeon.player;
		calculateOrbCounts();

		// Lightning calculations
		int currentLightningDamage = 0;
		for (AbstractOrb orb : player.orbs) {
			if (orb instanceof Lightning) {
				currentLightningDamage += orb.passiveAmount;
			}
		}

		int lightningDamageWithConsume = 0;
		if (hasLightning) {
			if (!(lastOrb instanceof Lightning)) {
				lightningDamageWithConsume = currentLightningDamage + (consumeValue * currentLightningOrbCount);
			} else {
				lightningDamageWithConsume = currentLightningDamage + (consumeValue * (currentLightningOrbCount - 1));
			}
		}
		int lightningDamageWithConsumeWithLastOrbAsLightning = lightningDamageWithConsume - lastOrb.passiveAmount; // might not be right

		int lightningValueWithConsume = (lastOrb instanceof Lightning) ? lightningDamageWithConsumeWithLastOrbAsLightning : lightningDamageWithConsume;

		int amountOfHitsPostConsume = (lastOrb instanceof Lightning) ? currentLightningOrbCount - 1 : currentLightningOrbCount;

		int amountOfHitsWithoutConsume = currentLightningOrbCount;

		if (firstOrb instanceof Lightning) {
			if (player.hasPower("Loop")) {
				if (player.maxOrbs > 1) {
					lightningValueWithConsume += (consumeValue + firstOrb.passiveAmount) * currentAmountOfLoops;
				}
				currentLightningDamage += firstOrb.passiveAmount * currentAmountOfLoops;
				if (player.maxOrbs > 1) {
					amountOfHitsPostConsume += currentAmountOfLoops;
				}
				amountOfHitsWithoutConsume += currentAmountOfLoops;
			}
			if (player.hasRelic("Cables")) { // Gold-plated Cables
				if (player.maxOrbs > 1) {
					lightningValueWithConsume += consumeValue + firstOrb.passiveAmount;
				}
				currentLightningDamage += firstOrb.passiveAmount;
				if (player.maxOrbs > 1) {
					amountOfHitsPostConsume += 1;
				}
				amountOfHitsWithoutConsume += 1;
			}
		}

		int valuePerLightningOrb = (hasLightning) ? getRandomOrb(OrbType.Lightning).passiveAmount : 0;

		Calculation lightningWithConsume = new Calculation(lightningValueWithConsume, amountOfHitsPostConsume, valuePerLightningOrb + consumeValue, OrbType.Lightning, true);
		calculations.add(lightningWithConsume);
		Calculation lightningWithoutConsume = new Calculation(currentLightningDamage, amountOfHitsWithoutConsume, valuePerLightningOrb, OrbType.Lightning, false);
		calculations.add(lightningWithoutConsume);

		if (lightningWithConsume.value > lightningWithoutConsume.value) {
			greaterDamage = true;
			equalDamage = false;
		} else if (lightningWithConsume.value < lightningWithoutConsume.value) {
			greaterDamage = false;
			equalDamage = false;
		} else if (lightningWithConsume.value == lightningWithoutConsume.value) {
			equalDamage = true;
			greaterDamage = false;
		}

		// Frost calculations
		int currentFrostBlock = 0;
		for (AbstractOrb orb : player.orbs) {
			if (orb instanceof Frost) {
				currentFrostBlock += orb.passiveAmount;
			}
		}

		int frostBlockWithConsume = 0;
		if (hasFrost) {
			if (!(lastOrb instanceof Frost)) {
				frostBlockWithConsume = currentFrostBlock + (consumeValue * currentFrostOrbCount);
			} else {
				frostBlockWithConsume = currentFrostBlock + (consumeValue * (currentFrostOrbCount - 1));
			}
		}

		int frostBlockWithConsumeWithLastOrbAsFrost = frostBlockWithConsume - lastOrb.passiveAmount;

		int frostValueWithConsume = (lastOrb instanceof Frost) ? frostBlockWithConsumeWithLastOrbAsFrost : frostBlockWithConsume;

		int amountOfBlocksPostConsume = (lastOrb instanceof Frost) ? currentFrostOrbCount - 1 : currentFrostOrbCount;

		int amountofBlocksWithoutConsume = currentFrostOrbCount;

		if (firstOrb instanceof Frost) {
			if (player.hasPower("Loop")) {
				if (player.maxOrbs > 1) {
					frostValueWithConsume += (consumeValue + firstOrb.passiveAmount) * currentAmountOfLoops;
				}
				currentFrostBlock += firstOrb.passiveAmount * currentAmountOfLoops;
				if (player.maxOrbs > 1) {
					amountOfBlocksPostConsume += currentAmountOfLoops;
				}
				amountofBlocksWithoutConsume += currentAmountOfLoops;
			}
			if (player.hasRelic("Cables")) { // Gold-plated Cables
				if (player.maxOrbs > 1) {
					frostValueWithConsume += consumeValue + firstOrb.passiveAmount;
				}
				currentFrostBlock += firstOrb.passiveAmount;
				if (player.maxOrbs > 1) {
					amountOfBlocksPostConsume += 1;
				}
				amountofBlocksWithoutConsume += 1;

			}
		}

		int valuePerFrostOrb = (hasFrost) ? getRandomOrb(OrbType.Frost).passiveAmount : 0;

		Calculation frostWithConsume = new Calculation(frostValueWithConsume, amountOfBlocksPostConsume, valuePerFrostOrb + consumeValue, OrbType.Frost, true);
		calculations.add(frostWithConsume);
		Calculation frostWithoutConsume = new Calculation(currentFrostBlock, amountofBlocksWithoutConsume, valuePerFrostOrb, OrbType.Frost, false);
		calculations.add(frostWithoutConsume);

		if (frostWithConsume.value > frostWithoutConsume.value) {
			greaterBlock = true;
			equalBlock = false;
		} else if (frostWithConsume.value < frostWithoutConsume.value) {
			greaterBlock = false;
			equalBlock = false;
		} else if (frostWithConsume.value == frostWithoutConsume.value) {
			equalBlock = true;
			greaterBlock = false;
		}
	}

	private AbstractOrb getRandomOrb(OrbType type) {
		if (type == OrbType.Lightning && !hasLightning) {
			return null;
		}
		if (type == OrbType.Frost && !hasFrost) {
			return null;
		}

		for (AbstractOrb orb : AbstractDungeon.player.orbs) {
			if (orb.ID.equals(type.toString())) {
				return orb;
			}
		}
		return null;
	}

	private void drawCalculations(SpriteBatch sb) {
		int y = 900;

		for (int i = 0; i < calculations.size(); i++) {
			int amountsX = 1820;

			Calculation calculation = calculations.get(i);

			// determine where to put the calculation values depending on how big the numbers are
			int calculationLength = (int) (Math.log10(calculation.value) + 1);
			int valueX = 1770 - ((calculationLength > 2) ? (10 * calculationLength) : 15);

			int amountsLength = (int) (Math.log10(calculation.valuePerOrb) + 1);
			if (calculation.amountOfOrbs == 10) {
				amountsLength++;
			}
			if (amountsLength > 1) {
				for (; amountsLength > 0; amountsLength--) {
					amountsX -= 7;
				}
			}
			if (calculation.amountOfOrbs == 10) {
				amountsX += 5;
			}

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append((calculation.withConsume) ? "with consume: " : "without consume: ");
			String type = (calculation.type == OrbType.Lightning) ? "Damage" : "Block";
			String text = stringBuilder.toString();

			Color color = determineColor(calculation);

			// Add some extra space between frost and lightning calculations
			if (i % (OrbType.values().length) == 0 && i != 0) {
				y -= 20;
			}

			// Draw the "damage" or "block" tag above the calculations
			if (calculation.withConsume) {
				FontHelper.renderFont(sb, font, type, 1470 * Settings.scale, y * Settings.scale, Color.GOLDENROD);
				y -= 25;
			}

			FontHelper.renderFont(sb, font, text, 1470 * Settings.scale, y * Settings.scale, color);
			FontHelper.renderFont(sb, font, "" + calculation.value, valueX * Settings.scale, y * Settings.scale, color);
			FontHelper.renderFont(sb, font, "(" + calculation.valuePerOrb + "*" + calculation.amountOfOrbs + ")", amountsX * Settings.scale, y * Settings.scale, color);

			y -= 35;
		}
	}

	private Color determineColor(Calculation calculation) {
		if (calculation.withConsume) {
			if (calculation.type == OrbType.Lightning) {
				if (equalDamage) {
					return Color.WHITE;
				} else if (greaterDamage) {
					return Color.GREEN;
				} else if (!greaterDamage) {
					return Color.RED;
				}
			} else { // Frost
				if (equalBlock) {
					return Color.WHITE;
				} else if (greaterBlock) {
					return Color.GREEN;
				} else if (!greaterBlock) {
					return Color.RED;
				}
			}
		} else { // Without consume
			if (calculation.type == OrbType.Lightning) {
				if (equalDamage) {
					return Color.WHITE;
				} else if (greaterDamage) {
					return Color.RED;
				} else if (!greaterDamage) {
					return Color.GREEN;
				}
			} else { // Frost
				if (equalBlock) {
					return Color.WHITE;
				} else if (greaterBlock) {
					return Color.RED;
				} else if (!greaterBlock) {
					return Color.GREEN;
				}
			}
		}
		return Color.BLACK; // If it ever returns black we've got an issue..
	}

	public boolean checkIfChangesOccurred() {
		if (!loaded) {
			return true;
		}
		if (previousTotalOrbCount != AbstractDungeon.player.orbs.size()) {
			previousTotalOrbCount = AbstractDungeon.player.orbs.size();
			return true;
		}
		if (previousFilledOrbCount != AbstractDungeon.player.filledOrbCount()) {
			previousFilledOrbCount = AbstractDungeon.player.filledOrbCount();
			return true;
		}
		if (AbstractDungeon.player.hasPower("Focus")) {
			if (previousFocus != AbstractDungeon.player.getPower("Focus").amount) {
				previousFocus = AbstractDungeon.player.getPower("Focus").amount;
				return true;
			}
		}
		if (previousUpgraded != upgradeToggle.isToggled()) {
			previousUpgraded = upgradeToggle.isToggled();
			return true;
		}
		currentAmountOfLoops = determineAmountOfLoops();
		if (currentAmountOfLoops != previousAmountOfLoops) {
			return true;
		}
		previousAmountOfLoops = currentAmountOfLoops;
		return false;
	}

	private void calculateOrbCounts() {
		hasLightning = false;
		hasFrost = false;
		currentLightningOrbCount = 0;
		currentFrostOrbCount = 0;
		AbstractPlayer player = AbstractDungeon.player;

		for (int i = 0; i < player.orbs.size(); i++) {
			AbstractOrb orb = player.orbs.get(i);
			if (i == 0) {
				firstOrb = orb;
			}
			if (orb instanceof Lightning) {
				hasLightning = true;
				currentLightningOrbCount++;
			} else if (orb instanceof Frost) {
				hasFrost = true;
				currentFrostOrbCount++;
			}
			if (i == player.orbs.size() - 1) {
				lastOrb = player.orbs.get(i);
			}
		}
	}

	public void show() {
		if (upgradeToggle == null) {
			upgradeToggle = new UpgradedConsumeToggle(ImageMaster.loadImage("img/ConsumeCalculatorUI/toggleButtonBase.png"),
					ImageMaster.loadImage("img/ConsumeCalculatorUI/toggleButtonOverlay2.png"),
					1470, 910, // x, y
					24, 24 // w, h
			);
		}
		showing = true;
	}

	private int determineAmountOfLoops() {
		ArrayList<AbstractPower> powers = AbstractDungeon.player.powers;
		for (AbstractPower power : powers) {
			if (power.ID.equals("Loop")) {
				return power.amount;
			}
		}
		return 0;
	}

	public void hide() {
		showing = false;
	}

	public boolean isShowing() {
		return showing;
	}

	public boolean canShow() {
		return canShow;
	}

	@Override
	public void receiveOnBattleStart(AbstractRoom ar) {
		canShow = true;
		previousFilledOrbCount = AbstractDungeon.player.filledOrbCount();
		calculate();
	}

	@Override
	public void receivePostBattle(AbstractRoom ar) {
		hide();
		canShow = false;
	}

	@Override
	public void receivePostUpdate() {
		try {
			if (Gdx.input.isKeyJustPressed(toggleKey)) {
				if (isShowing()) {
					hide();
				} else if (canShow()) {
					show();
				}
			}
		} catch (Exception ex) {
			System.out.println("Caught the following exception after cvc_toggleKey was pressed");
			ex.printStackTrace();
		}
	}
}
