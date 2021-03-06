package org.infinite.objects;

import java.util.ArrayList;
import java.util.Date;

import org.infinite.db.dao.DaoManager;
import org.infinite.db.dto.Area;
import org.infinite.db.dto.AreaItem;
import org.infinite.db.dto.Item;
import org.infinite.db.dto.Player;
import org.infinite.db.dto.PlayerKnowSpell;
import org.infinite.db.dto.PlayerOwnItem;
import org.infinite.db.dto.PlayerOwnQuest;
import org.infinite.db.dto.Spell;
import org.infinite.db.dto.SpellAffectPlayer;
import org.infinite.engines.AI.AiEngine;
import org.infinite.engines.fight.FightEngine;
import org.infinite.engines.fight.PlayerInterface;
import org.infinite.engines.items.ItemsEngine;
import org.infinite.engines.items.ItemsInterface;
import org.infinite.engines.magic.MagicEngine;
import org.infinite.engines.map.InaccessibleAreaException;
import org.infinite.engines.map.MapEngine;
import org.infinite.engines.quests.QuestsEngine;
import org.infinite.util.GenericUtil;
import org.infinite.util.InfiniteCst;

/**
 * @author msacchetti
 * 
 * This class implements the player's character.
 * Many settings comes from the player's actions on the UI and not from the AI engine (as per Monster class)
 * 
 * Both Monster and Character implements the PlayerInterface to be handled in the same way inside FightEngine
 * This let re-use fight engine for Player-Monster, Player-Player and (useful for testing) Monster-Monster battles
 * 
 * All possible methods are delegated to the various static Engines to keep the class as small as possible 
 * since many (one per player connected) Character instance will be stored in server memory at the same time  
 * 
 */
public class Character implements PlayerInterface, ItemsInterface {

	private DaoManager manager = null;
	private Player player = null;
	private int iAttackKind = InfiniteCst.ATTACK_TYPE_IDLE;

	/**
	 * Equipped items
	 */
	private PlayerOwnItem handRight = null;
	private PlayerOwnItem handLeft = null;	
	private PlayerOwnItem body = null;

	/**
	 * Backpack content
	 */
	private ArrayList<PlayerOwnItem> inventory = new ArrayList<PlayerOwnItem>();

	/**
	 * all Known spells, sorted by type 
	 */
	private ArrayList<PlayerKnowSpell> spellBookFight = new ArrayList<PlayerKnowSpell>();
	private ArrayList<PlayerKnowSpell> spellBookHeal = new ArrayList<PlayerKnowSpell>();
	private ArrayList<PlayerKnowSpell> spellBookProtect = new ArrayList<PlayerKnowSpell>();

	/**
	 * Spells prepared for fight
	 */
	private ArrayList<PlayerKnowSpell> preparedSpells = new ArrayList<PlayerKnowSpell>();

	/**
	 * Spells cast over player
	 */
	private ArrayList<SpellAffectPlayer> spellsAffecting = new ArrayList<SpellAffectPlayer>();

	/**
	 * duel sequence of attacks 
	 */
	private ArrayList<Object> battlePlan = new ArrayList<Object>();


	private ArrayList<PlayerOwnQuest> quests = new ArrayList<PlayerOwnQuest>();

	public Character(String name, String accountName,DaoManager daomanager,ItemsEngine itemsEngine,MagicEngine magicEngine) throws Exception{

		this.manager = daomanager;

		//get character Dao
		player = manager.getPlayerByName(accountName,name);

		//this one is useful if you change XP per level when already online, it will re align players level 
		int levelNow = player.getLevel();
		int levelNew = AiEngine.getLevelByPx( player.getPx() );
		if(levelNow != levelNew){
			player.setLevel(levelNew);
			GenericUtil.err("WARNING PX/Level disalignement: before:("+player.getPx()+"/"+levelNow+") , now("+player.getPx()+"/"+levelNew+")",null);
		}


		String battle = getDao().getBattle();

		//get inventory and assign
		ArrayList<PlayerOwnItem> poi = manager.getPlayerItems(getDao().getId());

		//equip all items without re-sync to DB
		for (int i = 0; i < poi.size(); i++) {
			equipItem(poi.get(i),itemsEngine);
		}

		//get spell book and assign
		ArrayList<PlayerKnowSpell> pks = manager.getPlayerSpells( getDao().getId());
		for (int i = 0; i < pks.size(); i++) {
			learnSpell( pks.get(i) , false,magicEngine );
		}

		//get spell cast on player
		long now = (new Date()).getTime();
		ArrayList<SpellAffectPlayer> sap = manager.getSpellsAffectingPlayer( getDao().getId() );
		for (int i = 0; i < sap.size(); i++) {

			//spell elapsed, removing
			if(sap.get(i).getElapsing() < now ){
				manager.delete( sap.get(i) );
			}
			else{ //storing over player
				getSpellsAffecting().add(sap.get(i));
			}


		}

		//convert battle plan to objects
		battlePlan = new ArrayList<Object>();
		setBattlePlan( deserializeBattlePlan(battle) );
		if(getBattlePlan().size()==0 && getHandRightPoi()!=null){
			changeFromBattlePlan("", "A"+getHandRightPoi().getId());
		}

	}



	/**
	 * An already owned item is moved to inventory (used to unequip items)
	 */
	public void moveToInventory(PlayerOwnItem poi,ItemsEngine itemsEngine) {
		itemsEngine.moveToInventory(this,poi,true);
	}

	/**
	 * A newly owned item is moved to inventory (used to loot/buy items)
	 */ 
	public void addToInventory(Item item,ItemsEngine itemsEngine){
		itemsEngine.addToInventory(this,item, true);
	}


	/**
	 * A newly owned item is moved to inventory (used to loot/buy items)
	 * NOTE: no persitence with this method 
	 */
	public void addToInventory(PlayerOwnItem poi){
		getInventory().add(poi);
	}

	/**
	 * dropping an owned item
	 */
	public void dropItem(PlayerOwnItem poi,ItemsEngine itemsEngine){
		itemsEngine.dropItem(this, poi);
	}

	/**
	 * Equip item on proper slot defined in PlayerOwnItem, if not set, it will be moved to inventory
	 * @see org.infinite.engines.items.ItemsInterface#equipItem(org.infinite.db.dto.PlayerOwnItem)
	 */
	public void equipItem(PlayerOwnItem poi,ItemsEngine itemsEngine){
		int previousId = itemsEngine.equipItem(this, poi);
		changeFromBattlePlan("A"+previousId,"A"+poi.getId());
	}

	/**
	 * Equip item on proper slot defined in PlayerOwnItem, if not set an exception will be thrown
	 * @see org.infinite.engines.items.ItemsInterface#wearItem(org.infinite.db.dto.PlayerOwnItem)
	 */
	public void wearItem(PlayerOwnItem poi,ItemsEngine itemsEngine) throws Exception{
		int previousId = itemsEngine.wearItem(this, poi);
		changeFromBattlePlan("A"+previousId,"A"+poi.getId());
	}


	public void  learnSpell(Spell spell,MagicEngine magicEngine) {
		magicEngine.learnSpell(this, spell);
	}

	public void learnSpell(PlayerKnowSpell pks,boolean persist,MagicEngine magicEngine){
		magicEngine.learnSpell(this,pks, persist);
	}

	public void addToPreparedSpells(PlayerKnowSpell pks){
		getPreparedSpells().add(pks);
	}

	/**
	 * Add spell to the list of spells affecting the player
	 * @see org.infinite.engines.fight.PlayerInterface#addToAffectingSpells(org.infinite.db.dto.Spell)
	 */
	public void addToAffectingSpells(Spell s,MagicEngine magicEngine){
		magicEngine.affectSpells(this,s,true);
	}

	/**
	 * Add spell to the list of spells affecting the player
	 * NOTE: <b>no persistence is applied with this method</b>
	 * @see org.infinite.engines.fight.PlayerInterface#addToAffectingSpells(org.infinite.db.dto.SpellAffectPlayer)
	 */
	public void addToAffectingSpells(SpellAffectPlayer sap){
		getSpellsAffecting().add( sap );
	}

	public int getBaseCA(){
		return (int)Math.round(InfiniteCst.FIGHT_BASE_CA + ( getDexterity()  / 5) );
	}

	public int getTotalCA(){
		return getBaseCA() + getArmorCA() + getShieldCA();
	}

	public int getArmorCA() {

		int ca = 0;
		if(getBody()!=null){
			ca = GenericUtil.getMaxRollDice( getBody().getDamage() );
		}
		return ca;
	}

	public int getShieldCA() {

		int ca = 0;
		if(getHandLeft()!=null && getHandLeft().getType()==InfiniteCst.EQUIP_ISSHIELD){
			ca = GenericUtil.getMaxRollDice( getHandLeft().getDamage() );
		}
		return ca;
	}




	public int getInitiative( int round ){
		// 1d6 random
		int init = GenericUtil.rollDice(1, 6, 0);

		int ind = round % getBattlePlan().size();

		iAttackKind = InfiniteCst.ATTACK_TYPE_IDLE;

		//TODO items
		if( getBattlePlan().get(ind) instanceof PlayerOwnItem ){
			iAttackKind = InfiniteCst.ATTACK_TYPE_WEAPON;
		}
		else{
			iAttackKind = InfiniteCst.ATTACK_TYPE_MAGIC;
		}

		//magic attack are based on intelligence, weapon and items on dexterity
		if(iAttackKind==InfiniteCst.ATTACK_TYPE_MAGIC)
		{
			init -= (int)Math.floor( getIntelligence()/5);
		}
		else
		{
			init -= (int)Math.floor( getDexterity()/5 );			
		}

		return init;  

	}

	public int getRollToAttack(){

		int cost = getHandRight()!=null?getHandRight().getCostAp():1;

		try {
			addActionPoints( -1 * cost);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int roll = GenericUtil.rollDice(1, 20, 0);
		roll +=  ( getStrenght()/5 );
		return roll;
	}

	public int inflictDamage(int dmg){
		int pl = -1;
		try {
			pl = addLifePoints( -1 * dmg);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pl;
	}

	public int healDamage(int heal) {

		int pl = -1;
		try {
			pl=  addLifePoints( heal );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		return pl;
	}

	public int restRound(int i) {
		int points = i * getDao().getLevel();
		try {
			addMagicPoints( points );
			addActionPoints( points );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return points;
	}

	public int getSpellDuration(){
		return getPreparedSpells().get(0).getSpell().getDuration();
	}


	public Object getCurrentAttack(int round){

		int ind = round % getBattlePlan().size();
		return getBattlePlan().get(ind);
	}


	public int getAttackDamage(int round){

		int dmg = 0;
		String szDice = "";

		//TODO getAttackDamage items
		if( getCurrentAttack(round) instanceof PlayerOwnItem ){
			szDice = ((PlayerOwnItem)getCurrentAttack(round)).getItem().getDamage();			
		}
		else{
			szDice = ((PlayerKnowSpell)getCurrentAttack(round)).getSpell().getDamage();
		}

		try {

			dmg += GenericUtil.rollDice( szDice ) ;
		} catch (Exception e) {
			GenericUtil.err("DICE:"+szDice,e);
		}

		return dmg;
	}

	public boolean isAlive(){
		return getPointsLife()>0?true:false;
	}

	public int getExperience(){
		return getDao().getPx();
	}
	
	public int getNextExperience(){
		return AiEngine.getLevelPx( getLevel()+1 );
	}

	@Override
	public int addExperience(int rewardPX){
		int currPx = getExperience() + rewardPX;

		if(currPx<0){
			currPx=0;
		}

		getDao().setPx(currPx);

		int next = AiEngine.getLevelPx( getLevel() );

		if(currPx>=next){
			getDao().setLevel( getLevel()+1 );
			getDao().setAssign( (short)(getPointsToAssing() + InfiniteCst.CFG_NEXTLEVELPOINTS) );
		}

		saveDao();

		return getExperience();

	}

	public float getGold(){
		return getDao().getGold();
	}

	@Override
	public float addGold(float rewardGold){
		float currGold = getGold() + rewardGold;

		if(currGold<0){
			currGold = 0;
		}

		getDao().setGold(currGold);
		saveDao();

		return getGold();

	}


	public ArrayList<Item> getRewardItems(){
		return null;
	}



	public String getName() {
		return getDao().getName();
	}

	public int getPointsLifeBase() {
		return getDao().getBasePl();
	}


	public int getPointsMagicBase() {
		return getDao().getBasePm();
	}


	public int getPointsActionBase() {
		return getDao().getBasePa();
	}

	public int getPointsCharmBase() {
		return getDao().getBasePc();
	}


	public Item getHandRight() {
		return handRight!=null?handRight.getItem():null;
	}


	public Item getHandLeft() {
		return handLeft!=null?handLeft.getItem():null;
	}


	public Item getBody() {
		return body!=null?body.getItem():null;
	}

	public PlayerOwnItem getHandRightPoi() {
		return handRight;
	}


	public PlayerOwnItem getHandLeftPoi() {
		return handLeft;
	}


	public PlayerOwnItem getBodyPoi() {
		return body;
	}

	public ArrayList<PlayerKnowSpell> getPreparedSpells(){
		return preparedSpells;
	}

	public int getAvailableSpellSlot(){
		return MagicEngine.getAvailableSpellSlots(this);
	}

	public int getAvailableAttackSlot(){
		return FightEngine.getAvailableAttackSlot(this) - getBattlePlan().size();
	}

	public String getPic() {
		return getDao().getImage();
	}

	public ArrayList<PlayerOwnItem> getInventory() {
		return inventory;
	}

	public ArrayList<PlayerKnowSpell> getSpellBookFight() {
		return spellBookFight;
	}

	public ArrayList<PlayerKnowSpell> getSpellBookHeal() {
		return spellBookHeal;
	}

	public ArrayList<PlayerKnowSpell> getSpellBookProtect() {
		return spellBookProtect;
	}

	public Player getDao(){
		return player;
	}

	private boolean saveDao(){
		return manager.update(getDao());
	}

	public int getPointsActionMax(){
		int mod = getDao().getBasePa();		
		mod += (getDexterity()/5);
		return mod;
	}

	public int getPointsLifeMax(){
		int mod = getDao().getBasePl();		
		mod += (getStrenght()/5);
		return mod;
	}

	public int getPointsCharmMax(){
		int mod = getDao().getBasePc();		
		mod += (getCharisma()/5);
		return mod;
	}

	public int getPointsMagicMax(){
		int mod = getDao().getBasePm();		
		mod += (getIntelligence()/5);
		return mod;
	}




	public int getDexterity(){

		int mod = getDao().getBaseDex();
		mod += getHandLeft()==null?0:getHandLeft().getModDex();
		mod += getHandRight()==null?0:getHandRight().getModDex();
		mod += getBody()==null?0:getBody().getModDex();

		for (int i = 0; i < spellsAffecting.size(); i++) {
			mod += spellsAffecting.get(i).getSpell().getModDex();
		}
		return mod;
	}

	public int getStrenght(){

		int mod = getDao().getBaseStr();
		mod += getHandLeft()==null?0:getHandLeft().getModStr();
		mod += getHandRight()==null?0:getHandRight().getModStr();
		mod += getBody()==null?0:getBody().getModStr();

		for (int i = 0; i < spellsAffecting.size(); i++) {
			mod += spellsAffecting.get(i).getSpell().getModStr();
		}
		return mod;
	}

	public int getCharisma(){

		int mod = getDao().getBaseCha();
		mod += getHandLeft()==null?0:getHandLeft().getModCha();
		mod += getHandRight()==null?0:getHandRight().getModCha();
		mod += getBody()==null?0:getBody().getModCha();

		for (int i = 0; i < spellsAffecting.size(); i++) {
			mod += spellsAffecting.get(i).getSpell().getModCha();
		}
		return mod;
	}

	public int getIntelligence(){

		int mod = getDao().getBaseInt();
		mod += getHandLeft()==null?0:getHandLeft().getModInt();
		mod += getHandRight()==null?0:getHandRight().getModInt();
		mod += getBody()==null?0:getBody().getModInt();

		for (int i = 0; i < spellsAffecting.size(); i++) {
			mod += spellsAffecting.get(i).getSpell().getModInt();
		}
		return mod;
	}


	@Override
	public int getLevel(){
		return getDao().getLevel();
	}





	public AreaItem getAreaItem() {		
		return getDao().getAreaItem();
	}

	public Area moveToAreaItem(AreaItem ai,MapEngine mapEngine) throws InaccessibleAreaException{
		return mapEngine.moveToAreaItem(this, ai);
	}



	@Override
	public int addLifePoints(int points) throws Exception {
		return setPointsLife( getPointsLife() + points );
	}

	@Override
	public int addMagicPoints(int points) throws Exception {
		return setPointsMagic( getPointsMagic() + points);
	}

	@Override
	public int addActionPoints(int points) throws Exception {
		return setPointsAction( getPointsAction() + points);
	}

	@Override
	public int addCharmPoints(int points) throws Exception {
		return setPointsCharm( getPointsCharm() + points);	
	}

	public int getPointsAction(){
		return getDao().getPa();
	}

	public int getPointsLife(){
		return getDao().getPl();
	}

	public int getPointsCharm(){
		return getDao().getPc();
	}

	public int getPointsMagic(){
		return getDao().getPm();
	}


	public int setPointsAction(int points) throws Exception{
		return setPointsAction(points, true);
	}

	private int setPointsAction(int points,boolean save) throws Exception{

		getDao().setPa( GenericUtil.forceBetween(points,0,getPointsActionMax())  );

		if(save){
			markForRegeneration();
			if(!saveDao()){
				throw new Exception("Could not save DAO object");
			}
		}
		return getDao().getPa();
	}

	public int setPointsLife(int points) throws Exception{
		return setPointsLife(points,true);
	}

	private int setPointsLife(int points, boolean save) throws Exception{

		getDao().setPl( GenericUtil.forceBetween(points,0,getPointsLifeMax()) );

		if(save){
			markForRegeneration();
			if(!saveDao()){
				throw new Exception("Could not save DAO object");
			}
		}
		return getDao().getPl();
	}

	public int setPointsCharm(int points) throws Exception{
		return setPointsCharm(points, true);
	}

	private int setPointsCharm(int points,boolean save) throws Exception{

		getDao().setPc( GenericUtil.forceBetween(points,0,getPointsCharmMax()) );		

		if(save){
			markForRegeneration();
			if(!saveDao()){
				throw new Exception("Could not save DAO object");
			}
		}
		return getDao().getPc();
	}

	public int setPointsMagic(int points) throws Exception{
		return setMagicPoints(points,true);
	}

	private int setMagicPoints(int points,boolean save) throws Exception{

		getDao().setPm( GenericUtil.forceBetween(points,0,getPointsMagicMax()) );

		if(save){
			markForRegeneration();
			if(!saveDao()){
				throw new Exception("Could not save DAO object");
			}
		}
		return getDao().getPm();
	}

	/**
	 * This method must mark the timestamp of the first subtraction to character's stats.
	 * this time will be used to evaluate regeneration rate on every request for character status
	 */
	private void markForRegeneration(){		

		//check if a previous modification exist
		if(getDao().getStatsMod()==0)
		{
			long now = 0;
			//check if some stat is not up to the maximum
			if( getDao().getPl()<getPointsLifeMax() ||	getDao().getPm()<getPointsMagicMax() ||
					getDao().getPa()<getPointsActionMax() || getDao().getPc()<getPointsCharmMax())
			{		

				now = (new Date()).getTime() ;
			}
			getDao().setStatsMod( now );
		}

	}

	public long getNexRegenereationTime(){

		//milliseconds to the next regeneration
		if(getDao().getStatsMod()==0){
			return 0;
		}
		long time = (getDao().getStatsMod() + InfiniteCst.CFG_CHAR_REGEN_TIME - (new Date()).getTime() );
		if(time<0){
			time = 0;
		}
		return time;
	}






	@Override
	public int getAttackType(PlayerInterface defender) {
		return iAttackKind;
	}

	public String[] getMeleeAttacks( int round){

		String out="";

		if(getHandRightPoi()==null){
			out = getDao().getAttack();
		}
		else{
			out = getHandRight().getName() + InfiniteCst.ATTACK_INNERSEPARATOR + 
			getHandRight().getImage() + InfiniteCst.ATTACK_INNERSEPARATOR +
			getHandRight().getDamage();

			if(getHandLeftPoi()!=null && getHandLeft().getType()==InfiniteCst.ITEM_TYPE_WEAPON){
				out += InfiniteCst.ATTACKS_SEPARATOR;
				out += getHandLeft().getName() + InfiniteCst.ATTACK_INNERSEPARATOR +
				getHandLeft().getImage() + InfiniteCst.ATTACK_INNERSEPARATOR +
				getHandLeft().getDamage();
			}

		}		

		return out.split(InfiniteCst.ATTACKS_SEPARATOR);
	}

	public String[] getAttackName( int round){

		String szName = "";
		String szImage = "";
		//TODO getAttackName items
		if( getCurrentAttack(round) instanceof PlayerOwnItem ){
			szName = ((PlayerOwnItem)getCurrentAttack(round)).getItem().getName();
			szImage = ((PlayerOwnItem)getCurrentAttack(round)).getItem().getImage();
		}
		else if( getCurrentAttack(round) instanceof PlayerKnowSpell ){
			szName = ((PlayerKnowSpell)getCurrentAttack(round)).getSpell().getName();
			szImage = ((PlayerKnowSpell)getCurrentAttack(round)).getSpell().getImage();
		}
		else{
			szName = "";
			szImage = "";
			iAttackKind = InfiniteCst.ATTACK_TYPE_IDLE;
		}

		return  new String[]{szName,szImage};
	}

	@Override
	public float getRewardGold() {
		// TODO Decide the amount of GOLD for defeating characters (should depend on levels difference)
		return 0;
	}

	@Override
	public int getRewardPX() {
		// TODO Decide the amount of PX for defeating characters
		return getLevel() * 100;
	}


	public boolean rollSavingThrow(Spell s, PlayerInterface caster,MagicEngine magicEngine) {
		return magicEngine.rollSavingThrow(s, caster, this);
	}


	public void setHandRight(PlayerOwnItem handRight) {
		this.handRight = handRight;
	}
	public void setHandLeft(PlayerOwnItem handLeft) {
		this.handLeft = handLeft;
	}
	public void setBody(PlayerOwnItem body) {
		this.body = body;
	}


	public Spell castSpell(Spell s,MagicEngine magicEngine) {
		return magicEngine.castSpell(this,s);
	}

	/**
	 * Set spell as available for being cast in duel
	 * @see org.infinite.engines.fight.PlayerInterface#prepareSpell(org.infinite.db.dto.PlayerKnowSpell)
	 */
	public void prepareSpell(PlayerKnowSpell pks,MagicEngine magicEngine) {
		magicEngine.prepareSpell(this,pks);		
	}

	/**
	 * Unset spell from being prepared, remove it from battle plan 
	 * @see org.infinite.engines.fight.PlayerInterface#unprepareSpell(int)
	 */
	public void unprepareSpell(int pksId,MagicEngine magicEngine) {
		magicEngine.unprepareSpell(this,pksId);	
		changeFromBattlePlan("s"+pksId, "");
	}

	/**
	 * This method takes the string with the list of attacks order from the db 
	 * and covert it to an array list of items/spell
	 * 
	 * Naming convention: 
	 *   A = attacks
	 *   S = Spells
	 *   I = Items
	 *   the number is object's id from inventory/book, NOT object's own item
	 */
	public ArrayList<Object> deserializeBattlePlan(String battle){
		// String example: A3,S15,I8,S5

		ArrayList<Object> out = new ArrayList<Object>();

		if(battle == null || battle.length()==0 ){
			return out;
		}

		String[] list = battle.split(",");
		for (int i = 0; i < list.length; i++) {

			if(list[i].length()<2){
				continue;
			}

			String type = list[i].substring(0,1);
			int num = GenericUtil.toInt(list[i].substring(1),-1);
			if(num==-1){
				GenericUtil.err("Invalid BattlePlan token ["+list[i]+"] in "+battle, null);
				continue;
			}

			if( type.equalsIgnoreCase("A") ){ //Attack may come only from Hands items

				if(getHandRightPoi()!=null && getHandRightPoi().getId() == num){
					out.add(getHandRightPoi());
				}
				else if(getHandLeftPoi()!=null && getHandLeftPoi().getId() == num && getHandLeft().getType()==InfiniteCst.ITEM_TYPE_WEAPON){
					out.add(getHandLeftPoi());
				}
			}
			else if( type.equalsIgnoreCase("S") ){ //Spells come from prepared spells 

				for (int j = 0; j < getPreparedSpells().size(); j++) {
					if(getPreparedSpells().get(j).getId()==num){
						out.add(getPreparedSpells().get(j) );
					}
				}		
			}
			else{
				//TODO deserialize item from battle plan
			}	
		}

		return out;
	}

	/**
	 * This method takes the list of attacks order and convert it to a string to be saved on db
	 */
	public String serializeBattlePlan(ArrayList<Object> list) {

		String out ="";

		for (int i = 0; i < list.size(); i++) {

			if( list.get(i) instanceof PlayerOwnItem){
				PlayerOwnItem poi = (PlayerOwnItem)list.get(i);
				if(getHandRightPoi()!=null && poi.getId() == getHandRightPoi().getId()){
					out += ",A"+poi.getId();
				}
				else if(getHandLeftPoi()!=null && poi.getId() == getHandLeftPoi().getId() && 
						getHandLeft().getType()==InfiniteCst.ITEM_TYPE_WEAPON ){
					out += ",A"+poi.getId();
				}
			}
			else if(list.get(i) instanceof PlayerKnowSpell){

				PlayerKnowSpell pks = (PlayerKnowSpell)list.get(i);
				for (int j = 0; j < getPreparedSpells().size(); j++) {
					if( getPreparedSpells().get(j).getId()==pks.getId()){
						out += ",S"+pks.getId();
					}
				}
			}
			else{
				//TODO serialize item to battle plan
			}			
		}

		//removing starting comma
		if(out.length()>0){
			out = out.substring(1);
		}
		return out;		
	}


	/**
	 * remove one object from the battle plan, add another at its place (if nothing removed, it's appended)
	 */
	protected void changeFromBattlePlan(String toRemove,String toAdd){

		String actual = serializeBattlePlan( getBattlePlan() );

		String newString ="";

		String toBeRemoved 	= (toRemove==null?"":toRemove);
		String toBeAdded 	= (toAdd==null?"":toAdd);
		
		if(toBeRemoved==null){
			toBeRemoved="";
		}

		if(toBeAdded==null){
			toBeAdded="";
		}

		toBeRemoved = toBeRemoved.toLowerCase();
		String[] list =  actual.toLowerCase().split(",");
		for (int i = 0; i < list.length; i++) {
			if( ! list[i].equals(toBeRemoved) ){
				newString += ","+list[i];
			}
			else{
				newString += ","+toBeAdded;
				toBeAdded = "";
			}
		}

		//if new (nothing to be removed) it had no match in the previous loop, appending
		if(toBeAdded.length()>0){
			newString += ","+toBeAdded;
		}

		newString = newString.replaceAll(",,", ",");

		if(newString.length()>0){
			newString = newString.substring(1);
		}


		setBattlePlan( deserializeBattlePlan(newString),false  );
		getDao().setBattle(newString);
		saveDao();
	}

	private void setBattlePlan(ArrayList<Object> battlePlan, boolean persist) {
		this.battlePlan = battlePlan;
		if(persist){
			getDao().setBattle( serializeBattlePlan( getBattlePlan() ));
			saveDao();
		}
	}

	public void setBattlePlan(ArrayList<Object> battlePlan) {
		setBattlePlan(battlePlan, true);
	}


	public ArrayList<Object> getBattlePlan() {
		return battlePlan;
	}



	public static Character checkForRegeneration(Character c,FightEngine fightEngine){

		long time = c.getDao().getStatsMod();

		//fist check if some cast spell expired
		fightEngine.checkForElapsedSpells(c);

		if(time!=0){

			long now = (new Date()).getTime();
			//eval how many time the regeneration time has elapsed
			int n = (int)Math.floor((now-time)/InfiniteCst.CFG_CHAR_REGEN_TIME) ;

			//at lease one regeneration occurs
			if(n>0){
				//regenerate 1pt per elapsed interval
				int ll = c.getDao().getPl() + n;
				int mm = c.getDao().getPm() + n;
				int aa = c.getDao().getPa() + n;
				int cc = c.getDao().getPc() + n;

				if(ll<c.getPointsLifeMax() || mm<c.getPointsMagicMax() || aa<c.getPointsActionMax() || cc < c.getPointsCharmMax())
				{
					//if still not at maximum, update time
					time = time + (n * InfiniteCst.CFG_CHAR_REGEN_TIME);
				}
				else{
					//else set timet to zero
					time = 0;
				}

				try {
					c.setPointsLife(ll,false);
					c.setMagicPoints(mm,false);
					c.setPointsAction(aa,false);
					c.setPointsCharm(cc,false);
					c.getDao().setStatsMod(time);
					c.saveDao();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


			}

		}		
		return c;
	}


	/**
	 * Check battleplan, if no weapon is set, parse default unarmed item.
	 * Check regeneration before start
	 * @see org.infinite.engines.fight.PlayerInterface#prepareForFight()
	 */
	public void prepareForFight(FightEngine fightEngine) {

		//update regeneration & affecting spells status
		checkForRegeneration(this,fightEngine);

		if( getBattlePlan().size() == 0 && getDao().getBattle().length()!=0 ){
			setBattlePlan( deserializeBattlePlan( getDao().getBattle() ) );			
		}

		if(getBattlePlan().size()==0 ){

			if( getHandRightPoi()!=null ){
				getBattlePlan().add(getHandRightPoi());
			}
			else{
				Item[] it = fightEngine.parseUnarmedAttack( getDao().getAttack() );
				for (int i = 0; i < it.length; i++) {
					PlayerOwnItem poi = new PlayerOwnItem(getDao(),it[i],0, InfiniteCst.EQUIP_HAND_RIGHT );
					getBattlePlan().add( poi );
				}

			}			
		}		
	}


	/**
	 * add all looted items to inventory
	 * @see org.infinite.engines.fight.PlayerInterface#lootItems(org.infinite.db.dto.Item[])
	 */
	public void lootItems(ArrayList<Item> rewardItems,ItemsEngine itemsEngine){

		if( rewardItems!=null && rewardItems.size()>0){

			for (int i = 0; i < rewardItems.size(); i++) {
				this.addToInventory(rewardItems.get(i),itemsEngine );
			}
		}

	}



	public void removeSpellsAffecting( int sapId ) {
		for (int i = 0; i < getSpellsAffecting().size(); i++) {
			if(getSpellsAffecting().get(i).getId() == sapId){
				getSpellsAffecting().remove(i);
				break;
			}
		} 
	}


	public ArrayList<SpellAffectPlayer> getSpellsAffecting() {
		return spellsAffecting;
	}



	public void upgradeStatus(int type) throws Exception {

		switch (type) {
		case InfiniteCst.STATUS_TYPE_PL:
			getDao().setBasePl( getDao().getBasePl() + 1 );
			break;
		case InfiniteCst.STATUS_TYPE_PM:
			getDao().setBasePm( getDao().getBasePm() + 1 );				
			break;
		case InfiniteCst.STATUS_TYPE_PA:
			getDao().setBasePa( getDao().getBasePa() + 1 );				
			break;
		case InfiniteCst.STATUS_TYPE_PC:
			getDao().setBasePc( getDao().getBasePc() + 1 );				
			break;
		case InfiniteCst.STATUS_TYPE_STR:
			getDao().setBaseStr( getDao().getBaseStr() + 1 );				
			break;
		case InfiniteCst.STATUS_TYPE_INT:
			getDao().setBaseInt( getDao().getBaseInt() + 1 );				
			break;
		case InfiniteCst.STATUS_TYPE_DEX:
			getDao().setBaseDex( getDao().getBaseDex() + 1 );				
			break;
		case InfiniteCst.STATUS_TYPE_CHA:
			getDao().setBaseCha( getDao().getBaseCha() + 1 );				
			break;

		default:
			throw new Exception("Invalid type!");
		}

		getDao().setAssign( (short) (getPointsToAssing() - 1) );
		saveDao();

	}



	public void payGold(float price) {

		float gold = getDao().getGold() - price;
		getDao().setGold(gold);
		saveDao();		
	}


	public void addQuests(PlayerOwnQuest poq) {
		quests.add(poq);
	}

	public ArrayList<PlayerOwnQuest> getQuests() {
		return quests;
	}
	
	public PlayerOwnQuest getQuestByQuestId(int questId) {
		
		PlayerOwnQuest poq = null;
		for(PlayerOwnQuest q: getQuests() ){
			if( q.getQuest().getId() == questId){
				poq = q;
				break;
			}
		}
		return poq;
	}
	
	public void removeFromQuest(int poqId) {

		for (int i = 0; i < getQuests().size(); i++) {
			if( getQuests().get(i).getId() == poqId){
				getQuests().remove(i);
				break;
			}			
		}		
	}
	
	public boolean isPlayerOnQuest(long questId, long questStatus) {
			
		boolean ret = false;
		
		for (PlayerOwnQuest q : quests) {
			if(q.getQuest().getId() == questId && q.getStatus()==questStatus){
				ret = true;
				break;
			}
		}
		
		//if players is not on quest and you are requesting it as not assigned, return true
		if(ret==false && questStatus==QuestsEngine.QUEST_STATUS_UNASSIGNED){
			ret = true;
		}
		
		return ret;
	}


	public short getPointsToAssing(){
		return getDao().getAssign();
	}



	@Override
	public boolean isMonster() {
		return false;
	}

}
