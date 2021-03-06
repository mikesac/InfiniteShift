package org.infinite.engines.items;

import org.infinite.db.dao.DaoManager;
import org.infinite.db.dto.Item;
import org.infinite.db.dto.PlayerOwnItem;
import org.infinite.engines.fight.PlayerInterface;
import org.infinite.objects.Character;
import org.infinite.util.InfiniteCst;

public class ItemsEngine {

	/* ***************** START SPRING BEANS ***************** */

	private DaoManager daoManager;

	public DaoManager getDaoManager() {return daoManager;}
	public void setDaoManager(DaoManager daoManager) {this.daoManager = daoManager;}

	/* ***************** END SPRING BEANS ***************** */


	public void persistOwnItem(PlayerOwnItem poi,int bodypart, int status){
		poi.setBodypart(bodypart);
		poi.setStatus(status);
		getDaoManager().update(poi);
	}

	public void moveToInventory(PlayerInterface player,PlayerOwnItem poi) {
		moveToInventory(player,poi,true);
	}

	public void moveToInventory(PlayerInterface player,PlayerOwnItem poi,boolean persist) {

		switch (poi.getBodypart()) {
		case InfiniteCst.EQUIP_BODY:
			player.setBody(null);
			break;
		case InfiniteCst.EQUIP_HAND_LEFT:
			player.setHandLeft(null);
			break;
		case InfiniteCst.EQUIP_HAND_RIGHT:
			player.setHandRight(null);
			break;

		default:
			break;
		}

		poi.setBodypart(InfiniteCst.EQUIP_STORE);
		player.addToInventory(poi);

		if(persist){
			getDaoManager().update(poi);
		}
	}

	public void addToInventory(PlayerInterface player,Item item){
		addToInventory(player,item, true);
	}

	public void addToInventory(PlayerInterface player,Item item,boolean persist) {

		PlayerOwnItem poi = null;
		if(player instanceof Character){			
			poi = new PlayerOwnItem(((Character)player).getDao(),item,0,InfiniteCst.EQUIP_STORE);
		}
		else{
			poi = new PlayerOwnItem( null,item,0,InfiniteCst.EQUIP_STORE);
		}

		player.addToInventory(poi);
		if( poi.getPlayer()!=null && persist){
			getDaoManager().create(poi);
		}
	}

	public void removeFromInventory(PlayerInterface player,int poiId) {

		for (int i = 0; i < player.getInventory().size(); i++) {

			if( player.getInventory().get(i).getId() == poiId){
				player.getInventory().remove(i);
				break;
			}			
		}		
	}


	public void dropItem(PlayerInterface player,PlayerOwnItem poi){
		removeFromInventory(player,poi.getId());
		getDaoManager().delete(poi);
	}

	public int equipItem(PlayerInterface player,PlayerOwnItem poi){

		int previousId = -1;
		switch ( poi.getBodypart() ) {
		case InfiniteCst.EQUIP_BODY:
			previousId = equipBody( player, poi , false);
			break;
		case InfiniteCst.EQUIP_HAND_LEFT:
			previousId = equipHandLeft( player,poi,false );
			break;
		case InfiniteCst.EQUIP_HAND_RIGHT:				
			previousId = equipHandRight( player,poi,false);
			break;
		default:
			moveToInventory( player,poi,false);
		break;
		}
		return previousId;
	}

	public int wearItem(PlayerInterface player,PlayerOwnItem poi) throws Exception {

		int previousId = -1; 
		if(poi.getItem().getType() == InfiniteCst.ITEM_TYPE_WEAPON){
			previousId = equipHandRight(player,poi);
		}
		else if ( poi.getItem().getType() == InfiniteCst.ITEM_TYPE_SHIELD) {
			previousId = equipHandLeft(player,poi);
		}
		else if(poi.getItem().getType() == InfiniteCst.ITEM_TYPE_ARMOR){
			previousId = equipBody(player,poi);
		}else{
			throw new Exception( "Cannot equip "+poi.getItem().getName() );
		}
		return previousId;
	}


	public int equipHandRight(PlayerInterface player,PlayerOwnItem poi) {	
		return equipHandRight(player,poi,true);
	}

	public int equipHandRight(PlayerInterface player,PlayerOwnItem poi, boolean persist) {	

		int previousId = -1;
		//if another item is equipped put in inventory
		PlayerOwnItem previous = player.getHandRightPoi(); 
		if( previous!=null){
			moveToInventory(player,previous);
			previousId = previous.getId();
		}

		player.setHandRight(poi);
		removeFromInventory(player,poi.getId());
		if(persist){
			persistOwnItem(poi, InfiniteCst.EQUIP_HAND_RIGHT, 0);
		}

		return previousId;
	}


	public int equipHandLeft(PlayerInterface player,PlayerOwnItem poi) {	
		return equipHandLeft(player,poi,true);
	}

	public int equipHandLeft(PlayerInterface player,PlayerOwnItem poi, boolean persist) {	

		int previousId = -1;
		//if another item is equipped put in inventory
		PlayerOwnItem previous = player.getHandLeftPoi(); 
		if( previous!=null){
			moveToInventory(player,previous);
			previousId = previous.getId();
		}

		player.setHandLeft(poi);
		if(persist){
			persistOwnItem(poi, InfiniteCst.EQUIP_HAND_LEFT, 0);
		}
		removeFromInventory(player,poi.getId());

		return previousId;
	}


	public int equipBody(PlayerInterface player,PlayerOwnItem poi) {	
		return equipBody(player,poi,true);
	}

	public int equipBody(PlayerInterface player,PlayerOwnItem poi, boolean persist) {	

		int previousId = -1;
		//if another item is equipped put in inventory
		PlayerOwnItem previous = player.getBodyPoi(); 
		if( previous!=null){
			moveToInventory(player,previous);
			previousId = previous.getId();
		}

		player.setBody(poi);
		if(persist){
			persistOwnItem(poi, InfiniteCst.EQUIP_BODY, 0);
		}
		removeFromInventory(player,poi.getId());
		return previousId;
	}


	public boolean canUseItem(Character c, Item i){

		boolean canUse=( 
				(i.getReqStr()<=c.getDao().getBaseStr()) &&
				(i.getReqInt()<=c.getDao().getBaseInt()) &&
				(i.getReqDex()<=c.getDao().getBaseDex()) &&
				(i.getReqCha()<=c.getDao().getBaseCha()) &&
				(i.getLevel()<=c.getLevel()) 
		);

		return canUse;
	}

	public void buyItem(Character c, Item item, float priceAdj) throws Exception{	

		boolean canBuy=( (c.getGold()>=( item.getPrice()/priceAdj) ) && canUseItem(c,item)	);
		if(canBuy){		
			try {
				c.addToInventory(item,this);
				c.payGold( item.getPrice() * priceAdj );
			} catch (Throwable e) {
				throw new Exception(e);
			}
		}
		else{
			throw new Exception("Character does not met item requirements or price");
		}
	}

	public void sellItem(Character c, PlayerOwnItem poi, float priceAdj) throws Exception{	

		float price = -1.0f * poi.getItem().getPrice() * priceAdj;
		try {
			c.dropItem(poi,this);
			c.payGold( price );
		} catch (Throwable e) {
			throw new Exception(e);
		}

	}

	
	public int getNumOfItemInInventory(PlayerInterface p, Item it){

		int out = 0;
		for(PlayerOwnItem poi : p.getInventory() ){
			if(poi.getItem().getId() == it.getId()){
				out++;
			}
		}
		
		return out;
	}
}
