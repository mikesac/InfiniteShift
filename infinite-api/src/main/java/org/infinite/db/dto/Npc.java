package org.infinite.db.dto;

// Generated 8-lug-2009 10.08.07 by Hibernate Tools 3.2.4.CR1

/**
 * Npc generated by hbm2java
 */
public class Npc implements java.io.Serializable {

	private static final long serialVersionUID = -6006619900013706731L;
	
	private Integer id;
	private Integer quest;
	private String name;
	private String image;
	private String description;
	private Integer baseStr;
	private Integer baseInt;
	private Integer baseDex;
	private Integer baseCha;
	private Integer basePl;
	private Integer basePm;
	private Integer basePa;
	private Integer basePc;
	private Integer level;
	private Integer px;
	private Integer status;
	private float gold;
	private short nitem;
	private boolean useWpn;
	private boolean useShld;
	private boolean useArm;
	private String dialog;
	private boolean ismonster;
	private Integer areatype;
	private Integer nattack;
	private String attack;

	public Npc() {
	}

	public Npc(String name, String image, String description, Integer baseStr,
			Integer baseInt, Integer baseDex, Integer baseCha, Integer basePl,
			Integer basePm, Integer basePa, Integer basePc, Integer level,
			Integer px, Integer status, float gold, short nitem,
			boolean useWpn, boolean useShld, boolean useArm, String dialog,
			boolean ismonster, Integer areatype, Integer nattack, String attack,Integer quest) {
		this.name = name;
		this.image = image;
		this.description = description;
		this.baseStr = baseStr;
		this.baseInt = baseInt;
		this.baseDex = baseDex;
		this.baseCha = baseCha;
		this.basePl = basePl;
		this.basePm = basePm;
		this.basePa = basePa;
		this.basePc = basePc;
		this.level = level;
		this.px = px;
		this.status = status;
		this.gold = gold;
		this.nitem = nitem;
		this.useWpn = useWpn;
		this.useShld = useShld;
		this.useArm = useArm;
		this.dialog = dialog;
		this.ismonster = ismonster;
		this.areatype = areatype;
		this.nattack = nattack;
		this.attack = attack;
		this.quest = quest;
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getQuest() {
		return this.quest;
	}

	public void setQuest(Integer quest) {
		this.quest = quest;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getImage() {
		return this.image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getBaseStr() {
		return this.baseStr;
	}

	public void setBaseStr(Integer baseStr) {
		this.baseStr = baseStr;
	}

	public Integer getBaseInt() {
		return this.baseInt;
	}

	public void setBaseInt(Integer baseInt) {
		this.baseInt = baseInt;
	}

	public Integer getBaseDex() {
		return this.baseDex;
	}

	public void setBaseDex(Integer baseDex) {
		this.baseDex = baseDex;
	}

	public Integer getBaseCha() {
		return this.baseCha;
	}

	public void setBaseCha(Integer baseCha) {
		this.baseCha = baseCha;
	}

	public Integer getBasePl() {
		return this.basePl;
	}

	public void setBasePl(Integer basePl) {
		this.basePl = basePl;
	}

	public Integer getBasePm() {
		return this.basePm;
	}

	public void setBasePm(Integer basePm) {
		this.basePm = basePm;
	}

	public Integer getBasePa() {
		return this.basePa;
	}

	public void setBasePa(Integer basePa) {
		this.basePa = basePa;
	}

	public Integer getBasePc() {
		return this.basePc;
	}

	public void setBasePc(Integer basePc) {
		this.basePc = basePc;
	}

	public Integer getLevel() {
		return this.level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public Integer getPx() {
		return this.px;
	}

	public void setPx(Integer px) {
		this.px = px;
	}

	public Integer getStatus() {
		return this.status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public float getGold() {
		return this.gold;
	}

	public void setGold(float gold) {
		this.gold = gold;
	}

	public short getNitem() {
		return this.nitem;
	}

	public void setNitem(short nitem) {
		this.nitem = nitem;
	}

	public boolean isUseWpn() {
		return this.useWpn;
	}

	public void setUseWpn(boolean useWpn) {
		this.useWpn = useWpn;
	}

	public boolean isUseShld() {
		return this.useShld;
	}

	public void setUseShld(boolean useShld) {
		this.useShld = useShld;
	}

	public boolean isUseArm() {
		return this.useArm;
	}

	public void setUseArm(boolean useArm) {
		this.useArm = useArm;
	}

	public String getDialog() {
		return this.dialog;
	}

	public void setDialog(String dialog) {
		this.dialog = dialog;
	}

	public boolean isIsmonster() {
		return this.ismonster;
	}

	public void setIsmonster(boolean ismonster) {
		this.ismonster = ismonster;
	}

	public Integer getAreatype() {
		return this.areatype;
	}

	public void setAreatype(Integer areatype) {
		this.areatype = areatype;
	}

	public Integer getNattack() {
		return this.nattack;
	}

	public void setNattack(Integer nattack) {
		this.nattack = nattack;
	}

	public String getAttack() {
		return this.attack;
	}

	public void setAttack(String attack) {
		this.attack = attack;
	}

}
