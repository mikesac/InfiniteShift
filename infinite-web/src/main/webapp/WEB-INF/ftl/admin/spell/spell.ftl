<#include "../../common/macro.ftl" >
<html>
<head>
	<title>Spells Editor</title>
</head>

<body>

<style>
td { white-space: nowrap; text-align:right;}
.ins{text-align:left;}
</style>
<table width="100%">
	<tr>
		<td width="70%" valign="top">
			<@nestedpanel icon="person" title="Spell Data" width=950 height=350>
			
				<form name="formedit" id="formedit">
					<table width="900px">
					<tr>
						<td>Id</td>
						<td class="ins"><input type="text" value="" size=3 name="s_id" readonly="readonly"></td>
						
						<td></td>
						<td>Name</td>
						<td colspan=2 class="ins"><input type="text" value="" name="s_name"></td>
						
						<td rowspan=2 colspan=2 align="right">
							<img src="../${pages.IMG_WEB_PATH}admin/spells.gif">
						</td>
						
					</tr>
					<tr>
						<td>Description</td>
						<td colspan=2 class="ins"><textarea name="s_description"></textarea></td>
						
						<td>Image</td>
						<td class="ins">
							<select name="s_image" onchange="updatePreview()">
								<option value="">No Image</option>
								<#list images as img>
								<option value="${img}">${img}</option>
								</#list>
							</select>
						</td>
						<td>
							<div id="preview" class="iconlarge" >
								<div class="tile"/>
							</div>
						</td>
					</tr>
					<tr>
												
						<td>Magic Points</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_costMp"></td>
						
						<td>Price</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_price"></td>
						
						
					</tr>
					
					<tr>
						<td>Required Lev</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_reqLev"></td>
						
						<td>Spell Level</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_level"></td>
						
						<td>Duration</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_duration"></td>
						
						<td>Initiative</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_initiative"></td>
						
					</tr>
					
					<tr>
						<td>Damage</td>
						<td class="ins"><input type="text" value="0d0" size=3 name="s_damage"></td>
						
						<td>Saving Throw</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_savingthrow"></td>
						
						<td>Spell Type</td>
						<td colspan="3" class="ins">
							<select name="s_spelltype">
								<option value="0">Attack</option>
								<option value="1">Healing</option>
								<option value="2">Protection</option>
							</select>
						</td>
					</tr>
										
					<tr>
						<td>Required Str</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_reqStr"></td>
						
						<td>Required Int</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_reqInt"></td>
						
						<td>Required Dex</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_reqDex"></td>
						
						<td>Required Cha</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_reqCha"></td>
					</tr>
					
					<tr>
						<td>Modified Str</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_modStr"></td>
						
						<td>Modified Int</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_modInt"></td>
						
						<td>Modified Dex</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_modDex"></td>
						
						<td>Modified Cha</td>
						<td class="ins"><input type="text" value="0" size=3 name="s_modCha"></td>
					</tr>					
					</table>
					
				</form>
				
				<table>
					<tr>
						<td>
							<form id="formsend" name="formsend" method="POST" action="${rc.getContextPath() + pages.ADMIN_SPELL+ pages.PAGE_EXT}">
								<input type="hidden" name="data" value="">
								<input type="hidden" name="act" value="0">
							</form>
							<center>							
								<button onclick="send(0)">Save</button>
								<button onclick="send(1)">Modify</button>
								<button onclick="send(2)">Delete</button>
								</center>
							</td><td><center>
								<span style="font-size:small;">
								Use this form to create/delete/edit spells for the game.<br/>You can create new by filling the form or get data from existing spells by clicking on the list on the right.
								</span>
								</center>
						</td>
					</tr>
				</table>		
			</@nestedpanel>
			<br/>
			<@nestedpanel icon="person" title="Upload New Spell Icon" width=950 height=80>
			
			<form name="uploadImage" method="POST" action="${rc.getContextPath() + pages.ADMIN_SPELL}.json" enctype="multipart/form-data">
				<table>
				<tr>
					<td>Icon name</td>
					<td><input type="text" name="icon"></td>
					
					<td>Image to upload</td>
					<td><input type="file" name="image"></td>
					<td><input type="submit" ></td>
				</tr>
				<tr>
					<td colspan=5>
					<center>
					<span style="font-size:small;">Use this form to upload new spells icon, once uploaded the icon will be available in the "image" combo on the main form.
					<br/>Uploaded Images will be automatically converted to ${pages.IMG_SPELL_EXT} format an resized to fit game needs.</span>
					</center>
					</td>
				</tr>
				</table>
			</form>
			
			</@nestedpanel>
		</td>
		<td width="30%">
				<table>
				<tr>
					<td>Filter
						<select id="list_type" onChange="reloadGrid()">
							<option value="-1">All Spells</option>
							<option value="0">Attack</option>
							<option value="1">Healing</option>
							<option value="2">Protection</option>
						</select>
					</td>
				<tr>
					<td>
						<table id="list" class="scroll" cellpadding="0" cellspacing="0">
							<tr><td>Loading data...</td></tr>
						</table>
					</td>
				</tr>
			</table>
		</td>
	</tr>
</table>
<link rel="stylesheet" type="text/css" href="../../js/jquery/grid/ui.jqgrid.css">
<script type="text/javascript" src="../../js/jquery/grid/jquery.jqGrid.min.js"></script>
<script>

function reloadGrid()
{

	var el = document.getElementById('list_type');
	f= el.options[el.selectedIndex].value;
	
	$('#list').setGridParam({url:"${rc.getContextPath() + pages.ADMIN_SPELL}.json?id=-99&f="+f});
    $('#list').trigger("reloadGrid");
}


function initGrid()
{
	var el = document.getElementById('list_type');
	f= el.options[el.selectedIndex].value;
	
	jQuery("#list").jqGrid({
		  url:"${rc.getContextPath() + pages.ADMIN_SPELL}.json?id=-99&f="+f, 
		 datatype: "json", 
		 width:380,
		 forceFit:true,
		 hidegrid:false,
		 height: 500, 
		 colNames:['Image', 'Name'], 
		 colModel:[ 
		 	{name:'img',index:'img', width:60, sorttype:"string",resizable:false}, 
		 	{name:'name',index:'name', sorttype:"string",resizable:false}, 
		 ], 
		 caption: "Spell List" }); 
		  
}

function loadData(id){

	$.getJSON( 
		"${rc.getContextPath() + pages.ADMIN_SPELL}.json?id="+id,
        function(jdata){
        	for(var i=0;i<document.forms['formedit'].elements.length;i++){
					var name = document.forms['formedit'].elements[i].name;

					if(name==""){
						continue;
					}
					
					if(document.forms['formedit'].elements[name].type=="checkbox"){
						document.forms['formedit'].elements[name].checked=jdata[name];
					}
					else{						
						document.forms['formedit'].elements[name].value = jdata[name];
					}
				}
			updatePreview();
        }
	);	
}

function prepareForm(){

    var o = "";
    var a = $("#formedit").serializeArray();
    $.each(a, function() {
        o = o +  ",\"" + this.name + "\":\"" + (this.value || "") + "\"";
        }
    );
    
    o = o.substring(1)
   
    return "{"+o+"}";
}
	



function send(act){
	document.forms['formsend'].elements["act"].value = act;
	document.forms['formsend'].elements["data"].value = prepareForm( )
		
	document.forms['formsend'].submit()
	document.forms['formsend'].reset()
	document.forms['formedit'].reset()
}

function updatePreview(){

	var preview = document.forms['formedit'].s_image.value;

	if(preview!=""){
		preview = "${pages.IMG_SPELL_PATH}"+preview+"${pages.IMG_SPELL_EXT}"
	}

	document.getElementById('preview').style.backgroundImage="url("+preview+")";
}

initGrid();
</script>
</body>
</html>