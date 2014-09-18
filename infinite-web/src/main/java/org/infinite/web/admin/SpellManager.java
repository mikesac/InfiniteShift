package org.infinite.web.admin;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.infinite.db.dao.DaoManager;
import org.infinite.db.dto.Spell;
import org.infinite.db.dto.TomcatRoles;
import org.infinite.util.GenericUtil;
import org.infinite.web.utils.ControllerUtils;
import org.infinite.web.utils.PagesCst;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class SpellManager {

	@Autowired
	private	PagesCst pages;

	@Autowired
	private DaoManager daoManager;

	public void setPages(PagesCst pages) {this.pages = pages;}
	public PagesCst getPages() {return pages;}

	public void setDaoManager(DaoManager dao) {this.daoManager = dao;}
	public DaoManager getDaoManager() { return daoManager; }

	@RequestMapping(value="/admin/spell/spell.html",method = RequestMethod.GET)
	public ModelAndView initSpells(HttpServletRequest request, HttpServletResponse resp, ModelMap model)
	{
		String userName = request.getUserPrincipal().getName();		
		TomcatRoles role = getDaoManager().getUserRole(userName);		
		if(role==null || !role.getRole().equalsIgnoreCase("manager")){
			return new ModelAndView( getPages().getRedirect(request,getPages().getPAGE_ROOT(),"You cannot access this area!") );
		}
		
		model = getPages().initController(model, request);
		
		model = prepareModel(request, model);
		return new ModelAndView( getPages().getADMIN_SPELL(),model );
	}



	@RequestMapping(value="/admin/spell/spell.json",method = RequestMethod.GET)
	public ModelAndView getJson(
			HttpServletRequest request, HttpServletResponse resp, ModelMap model,
			@RequestParam(value="id",required=true) int id,
			@RequestParam(value="f",required=false) Integer type
	)
	{

		if(id<0){
			ArrayList<Spell> list = getDaoManager().getSpellListByType(type);
			model.addAttribute("spells", list);
			model.addAttribute("grid", true);
			model.addAttribute("pages", getPages());
		}
		else{
			Spell s = getDaoManager().getSpellById(id);
			model.addAttribute("s",s);
			model.addAttribute("grid", false);
		}

		return new ModelAndView( getPages().getADMIN_SPELL_JSON(),model );
	}

	
	


	@RequestMapping(value="/admin/spell/spell.html",method = RequestMethod.POST)
	public ModelAndView saveSpell(HttpServletRequest request, HttpServletResponse resp, ModelMap model,
			@RequestParam(value="act",required=true) int act,
			@RequestParam(value="data",required=true) String data)
	{

		String msg = "";
		try{
			JSONParser parser=new JSONParser();

			JSONObject obj =(JSONObject)parser.parse(data);

			int id = GenericUtil.toInt( (String) obj.get("s_id"), -1);
			Spell s;
			switch (act) {
			case 0: //save
				s= new Spell();
				s = (Spell) ControllerUtils.populateObject(s,obj,"s_");
				getDaoManager().create(s);
				msg = "New Spell ["+s.getName()+"] Created";
				break;
			case 1:	//edit					
				s = getDaoManager().getSpellById(id);
				s = (Spell) ControllerUtils.populateObject(s,obj,"s_");
				getDaoManager().update(s);
				msg = "Spell ["+s.getName()+"] Updated";
				break;				
			case 2:	//delete
				s = getDaoManager().getSpellById(id);
				getDaoManager().delete( s );
				msg = "Spell Deleted";
				break;

			default:
				break;
			}


		} catch (Exception e) 
		{
			model.addAttribute(getPages().getCONTEXT_ERROR(),e.getMessage());
		}

		model.addAttribute(getPages().getCONTEXT_ERROR(),msg);

		model = prepareModel(request, model);

		return new ModelAndView( getPages().getADMIN_SPELL(),model );
	}


	



	@RequestMapping(value="/admin/spell/spell.json",method = RequestMethod.POST)
	public ModelAndView uploadImage(HttpServletRequest request, HttpServletResponse resp, ModelMap model)
	{
		try{
			ControllerUtils.uploadAndResizeImage(request,
					getPages().getAbsoluteIMG_SPELL_PATH(),null,
					getPages().getIMG_SPELL_EXT().substring(1),
					56,56);
			

		}
		catch (Exception e) {
			return new ModelAndView( getPages().getRedirect(request,getPages().getADMIN_SPELL(),e.getMessage()) );
		}

		return new ModelAndView( getPages().getRedirect(request,getPages().getADMIN_SPELL(),"File succesfully uploaded!") );
	}
	




	private ModelMap prepareModel(HttpServletRequest request, ModelMap model) 
	{
		ArrayList<String> l = ControllerUtils.getFileList(getPages().getAbsoluteIMG_SPELL_PATH());
		model.addAttribute("images", l);

		ArrayList<Spell> list = getDaoManager().getSpellList();
		model.addAttribute("spells", list);

		model.addAttribute(getPages().getCONTEXT_PAGES(), getPages());
		model.addAttribute("base", request.getContextPath());

		return model;
	}

	
}
