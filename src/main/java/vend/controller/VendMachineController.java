package vend.controller;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSONObject;

import base.util.CacheUtils;
import base.util.DateUtil;
import base.util.Function;
import base.util.JsonUtil;
import base.util.Page;
import vend.entity.CodeLibrary;
import vend.entity.VendAd;
import vend.entity.VendGoods;
import vend.entity.VendMachine;
import vend.entity.VendMachineInt;
import vend.entity.VendOrder;
import vend.entity.VendShopQrcode;
import vend.entity.VendUser;
import vend.service.CodeLibraryService;
import vend.service.VendAdService;
import vend.service.VendGoodsService;
import vend.service.VendMachineIntService;
import vend.service.VendMachineService;
import vend.service.VendOrderService;
import vend.service.VendShopQrcodeService;
import vend.service.VendUserService;

@Controller
@RequestMapping("/machine")
public class VendMachineController{
	public static Logger logger = Logger.getLogger(VendMachineController.class);
	@Autowired
	VendMachineService vendMachineService;
	@Autowired
	VendMachineIntService vendMachineIntService;
	@Autowired
	VendAdService vendAdService;
	@Autowired
	VendGoodsService vendGoodsService;
	@Autowired
	VendUserService vendUserService;
	@Autowired
	VendShopQrcodeService vendShopQrcodeService;
	@Autowired
	CodeLibraryService codeLibraryService;
	@Autowired
	VendOrderService vendOrderService;
	/**
	 * 根据输入信息条件查询机器列表，并分页显示
	 * @param model
	 * @param vendMachine
	 * @param page
	 * @param request
	 * @return
	 */
	@RequiresPermissions({"machine:machines"})
	@RequestMapping(value="/machines")
	public String listVendMachine(Model model,@ModelAttribute VendMachine vendMachine, @ModelAttribute Page page,HttpServletRequest request) {
		String currentPageStr = request.getParameter("currentPage");
		logger.info(currentPageStr + "===========");
		if(currentPageStr != null){
			int currentPage = Integer.parseInt(currentPageStr);
			page.setCurrentPage(currentPage);
		}
		logger.info(page.toString());
		logger.info(vendMachine.toString());
		HttpSession session=request.getSession();
		VendUser user=(VendUser)session.getAttribute("vendUser");
		List<CodeLibrary> usestatus=codeLibraryService.selectByCodeNo("USESTATUS");
		model.addAttribute("usestatus", usestatus);
		
		if(vendMachine.getMachineCode()!=null){
			model.addAttribute("machineCode",vendMachine.getMachineCode());
		}
		if(vendMachine.getMachineName()!=null){
			model.addAttribute("machineName",vendMachine.getMachineName());
		}
		
		List<VendMachine> vendMachines = vendMachineService.listVendMachine(vendMachine, page);
		String userlist="";
		if(user!=null&&user.getUsercode()!=null){//上级账号
			userlist=vendUserService.getNextUsersOwnSelf(user.getUsercode());
		}
		List<VendMachine> vendMachines1=new ArrayList<VendMachine>();
		if(userlist!=null){
			for(VendMachine vendMachine1:vendMachines){
				if(userlist.indexOf(vendMachine1.getUsercode())!=-1){
					vendMachines1.add(vendMachine1);
				}
			}
		}
		model.addAttribute("page", page);
		model.addAttribute("vendMachines",vendMachines1);
		return "manage/machine/machine_list";
	}
	/**
	 * 开机
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/{id}/open")
	public String openMachine(@PathVariable int id){
		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine!=null){
			vendMachine.setUseStatus("1");
			vendMachineService.editVendMachine(vendMachine);
		}
		return "redirect:../machines"; 
	}
	/**
	 * 关机
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/{id}/shutdown")
	public String shutDownMachine(@PathVariable int id){
		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine!=null){
			vendMachine.setUseStatus("2");
			vendMachineService.editVendMachine(vendMachine);
		}
		return "redirect:../machines"; 
	}
	/**
	 * 重启
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/{id}/reboot")
	public String reBootMachine(@PathVariable int id){
		//VendMachine vendMachine=vendMachineService.getOne(id);
		return "redirect:../machines"; 
	}
	/**
	 * 详情
	 * @param model
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/{id}/detail")
	public String detail(Model model,@PathVariable int id){
		List<VendShopQrcode> clientQrcodes=vendShopQrcodeService.selectByType("2");
		model.addAttribute("clientQrcodes", clientQrcodes);
		VendMachine vendMachine=vendMachineService.getOne(id);
		model.addAttribute("vendMachine", vendMachine);
		return "manage/machine/machine_detail"; 
	}
	/**
	 * 跳转机器信息添加界面
	 * @param model
	 * @return
	 */
	@RequiresPermissions({"machine:add"})
	@RequestMapping(value="/add",method=RequestMethod.GET)
	public String machined(Model model){
		model.addAttribute(new VendMachine());
		return "manage/machine/machine_add";
	}
   /**
    * 添加机器信息
    * @param request
    * @param model
    * @param vendMachine
    * @param br
    * @return
    */
	@RequiresPermissions({"machine:add"})
    @RequestMapping(value="/add",method=RequestMethod.POST)
	public String machined(HttpServletRequest request,Model model,@Validated VendMachine vendMachine,BindingResult br){
		VendMachine vendMachine1=vendMachineService.selectByMachineId(vendMachine.getMachineId());
		if(vendMachine1!=null){
			br.rejectValue("machineId", "MACHINEIDREPPEAT", "该机器ID已被绑定");
		}
    	if(br.hasErrors()){
    		return "manage/machine/machine_add";
    	}
    	vendMachine.setMachineType("gyp206");
    	vendMachineService.insertVendMachine(vendMachine);
    	return "redirect:machines";
	}
    /**
	 * 跳转投放广告界面
	 * @param model
	 * @return
	 */
	@RequiresPermissions({"machine:adputon"})
	@RequestMapping(value="/{id}/adputon",method=RequestMethod.GET)
	public String adPuton(Model model,@PathVariable int id){
		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine!=null&&vendMachine.getMachineId()!=null){
			VendAd vendAd=vendAdService.selectByMachineId(vendMachine.getMachineId());
			if(vendAd!=null){
				model.addAttribute(vendAd);
			}else{
				vendAd=new VendAd();
				model.addAttribute(vendAd);
			}
			List<CodeLibrary> adscreens=codeLibraryService.selectByCodeNo("ADSCREEN");
			String adtypename="";
			for(CodeLibrary codeLibrary:adscreens){
				if(vendAd.getExtend2()!=null&&codeLibrary.getItemno().equals(vendAd.getExtend2())){
					adtypename=codeLibrary.getItemname();
				}
			}
			model.addAttribute("adtypename", adtypename);
			model.addAttribute("adscreens", adscreens);
		}
		
		model.addAttribute("id",id);
    	List<CodeLibrary> uppictypes=codeLibraryService.selectByCodeNo("UPPICTYPE");
		model.addAttribute("uppictypes", uppictypes);
		List<CodeLibrary> upvideotypes=codeLibraryService.selectByCodeNo("UPVIDEOTYPE");
		model.addAttribute("upvideotypes", upvideotypes);
		return "manage/machine/machine_adputon";
	}
	/**
	 * 跳转投放商户二维码界面
	 * @param model
	 * @return
	 */
	@RequiresPermissions({"machine:qrcodeputon"})
	@RequestMapping(value="/{id}/qrcodeputon",method=RequestMethod.GET)
	public String qrcodePuton(Model model,@PathVariable int id){
		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine!=null&&vendMachine.getMachineId()!=null){
			VendShopQrcode vendShopQrcode=vendShopQrcodeService.selectByMachineId(vendMachine.getMachineId());
			if(vendShopQrcode!=null){
				model.addAttribute(vendShopQrcode);
			}else{
				vendShopQrcode=new VendShopQrcode();
				model.addAttribute(vendShopQrcode);
			}
		}
		List<CodeLibrary> uppictypes=codeLibraryService.selectByCodeNo("UPPICTYPE");
		model.addAttribute("uppictypes", uppictypes);
		model.addAttribute("id",id);
		return "manage/machine/machine_qrcodeputon";
	}
	/**
	 * 修改机器信息
	 * @param request
	 * @param model
	 * @param vendMachine
	 * @param br
	 * @return
	 */
	@RequiresPermissions({"machine:edit"})
    @RequestMapping(value="/edit",method=RequestMethod.POST)
	public String edit(HttpServletRequest request,Model model,@Validated VendMachine vendMachine,BindingResult br){
    	if(br.hasErrors()){
    		return "manage/machine/machine_edit";
    	}
    	vendMachineService.editVendMachine(vendMachine);
		return "redirect:machines";
	}
    /**
     * 删除机器信息
     * @param user
     * @param br
     * @return
     */
	@RequiresPermissions({"machine:del"})
    @RequestMapping(value="/{id}/del")
 	public String del(@PathVariable Integer id){
    	vendMachineService.delVendMachine(id);;
 		return "redirect:/machine/machines";
 	}
    /**
     * 批量删除机器信息
     * @param ids
     * @return
     */
	@RequiresPermissions({"machine:dels"})
    @RequestMapping(value="/dels")
  	public String dels(HttpServletRequest request){
    	String ids=request.getParameter("ids");
    	String idArray[]=ids.split(",");
    	int[] idArray1=new int[idArray.length];
    	for(int i=0;i<idArray.length;i++){
    		idArray1[i]=Function.getInt(idArray[i], 0);
    	}
    	vendMachineService.delVendMachines(idArray1);
  		return "redirect:/machine/machines";
  	}
	/**
	 * 解绑机器
	 * @param id
	 * @param transusercode
	 * @param response
	 * @throws IOException 
	 */
	@RequiresPermissions({"machine:unbind"})
    @RequestMapping(value="/{id}/{transusername}/unbind")
  	public String unbind(@PathVariable Integer id,@PathVariable String transusername,HttpServletResponse response) throws IOException{
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
	    json.put("success", 0);
	    json.put("msg", "解绑失败");
		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine==null){
			json.put("success", 0);
			json.put("msg", "该机器不存在");
			response.getWriter().append(json.toJSONString());
			return null;
		}
		
		VendUser vendUser=vendUserService.selectByUsername(transusername);
		if(vendUser==null){
			json.put("success", 0);
			json.put("msg", "要转移的商户不存在");
			response.getWriter().append(json.toJSONString());
			return null;
		}
		if(vendUser.getRoleId()!=4){
			json.put("success", 0);
			json.put("msg", "要转移的用户必须是商家用户");
			response.getWriter().append(json.toJSONString());
			return null;
		}
		
		if(vendUser.getUsercode()==null){
			json.put("success", 0);
			json.put("msg", "要转移的商户不存在");
			response.getWriter().append(json.toJSONString());
			return null;
		}
		
		vendMachine.setUsercode(vendUser.getUsercode());
    	int isOk=vendMachineService.editVendMachine(vendMachine);
    	if(isOk==1){
    		json.put("success", 1);
			json.put("msg", "解绑成功");
    		CacheUtils.clear();
    	}

		response.getWriter().append(json.toJSONString());
		return null;

  	}
	/**
	 * 每个机器销售统计
	 * @param model
	 * @param vendMachine
	 * @param page
	 * @param request
	 * @return
	 */
	@RequestMapping(value="/sales")
	public String sales(Model model,@ModelAttribute VendMachine vendMachine, @ModelAttribute Page page,HttpServletRequest request) {
		String currentPageStr = request.getParameter("currentPage");
		logger.info(currentPageStr + "===========");
		if(currentPageStr != null){
			int currentPage = Integer.parseInt(currentPageStr);
			page.setCurrentPage(currentPage);
		}
		logger.info(page.toString());
		logger.info(vendMachine.toString());
		HttpSession session=request.getSession();
		VendUser user=(VendUser)session.getAttribute("vendUser");
		List<CodeLibrary> usestatus=codeLibraryService.selectByCodeNo("USESTATUS");
		model.addAttribute("usestatus", usestatus);
		List<VendMachine> vendMachines = vendMachineService.listVendMachine(vendMachine, page);
		String userlist="";
		if(user!=null&&user.getUsercode()!=null){//上级账号
			userlist=vendUserService.getNextUsersOwnSelf(user.getUsercode());
		}
		List<VendMachine> vendMachines1=new ArrayList<VendMachine>();
		if(userlist!=null){
			for(VendMachine vendMachine1:vendMachines){
				if(userlist.indexOf(vendMachine1.getUsercode())!=-1){
					vendMachines1.add(vendMachine1);
				}
			}
		}
		
		String beginTime=request.getParameter("beginTime");
		if(beginTime==null){
			beginTime=DateUtil.getCurrentDateStr();
		}
		String endTime=request.getParameter("endTime");
		if(endTime==null){
			endTime=DateUtil.format(DateUtil.addDays(DateUtil.parseDate(DateUtil.getCurrentDateStr()),1));
		}
		List<Map<String,Object>> list=new ArrayList<Map<String,Object>>();//每个机子的销售统计集合
		for(VendMachine vendMachine2:vendMachines1){
			if(vendMachine2!=null&&vendMachine2.getMachineCode()!=null){
				String mochinecodeArray[]={vendMachine2.getMachineCode()};
			    VendOrder vendOrder=new VendOrder();
			    vendOrder.setExtend1("1");
			    List<VendOrder> vendOrders=vendOrderService.selectByParams(vendOrder,mochinecodeArray,beginTime, endTime);
			    
			    int user_num=0;//消费用户数
			    int sell_num=0;//销售量
			    double sell_amount=0.0;//销售金额
			    int free_num=0;//免费数量
			    String buyusers="";
			    for(VendOrder vendOrder1:vendOrders){
			    	if(vendOrder1!=null&&vendOrder1.getUsercode()!=null){
			    		if(buyusers.indexOf(vendOrder1.getUsercode()+",")==-1){
			    			buyusers+=vendOrder1.getUsercode()+",";
			    			user_num++;
			    		}
			    	}
			    	if(vendOrder1!=null&&vendOrder1.getNum()!=null){
			    		    sell_num+=vendOrder1.getNum();
			    	}
			    	if(vendOrder1!=null&&vendOrder1.getAmount()!=null){
			    		    sell_amount+=vendOrder1.getAmount().doubleValue();
			    	}
			    	if(vendOrder1!=null&&vendOrder1.getFreeStatus()!=null&&vendOrder1.getFreeStatus().equals("1")){
			    		    free_num++;
		    	    }
			    }
			    Map<String,Object> map=new HashMap<String,Object> ();
			    map.put("machinecode", vendMachine2.getMachineCode());
			    map.put("user_num", user_num);
			    map.put("sell_num", sell_num);
			    map.put("sell_amount", sell_amount);
			    map.put("free_num", free_num);
			    list.add(map);
			}
		}
		model.addAttribute("page", page);
		model.addAttribute("list",list);
		return "manage/machine/machine_sales";
	}
	/**
	 * 进到生成机器ID界面
	 * @return
	 */
	@RequestMapping(value="/togenerateId")
	public String generateId(){
		return "manage/machine/machine_generateid";
	}
	/**
	 * 生成机器ID
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value="/generateId",method=RequestMethod.POST)
	public void generateId(HttpServletResponse response) throws IOException{
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		json.put("machineId", "");
		String machineId=Function.getmachineId();
		json.put("machineId", machineId);
		response.getWriter().append(json.toJSONString());
	}
	/**
	 * 跳到初始化机器信息界面
	 * @param model
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/{id}/initialize",method=RequestMethod.GET)
	public String initialize(Model model,@PathVariable int id){
		List<VendGoods> vendGoodss=vendGoodsService.findAll();
		List<VendMachineInt> vendMachineInts=vendMachineIntService.selectByBelongMachine(id);
		if(vendMachineInts.size()==0){
			for(VendGoods vendGoods:vendGoodss){
				VendMachineInt vendMachineInt=new VendMachineInt();
				vendMachineInt.setBelongMachine(id);
				vendMachineInt.setGoodsId(vendGoods.getId());
				vendMachineInt.setGoodsName(vendGoods.getGoodsName());
				vendMachineInt.setHotStatus("0");
				vendMachineIntService.insertVendMachineInt(vendMachineInt);
				vendMachineInts.add(vendMachineInt);
				
				vendMachineInt.setBelongMachine(id);
				vendMachineInt.setGoodsId(vendGoods.getId());
				vendMachineInt.setGoodsName(vendGoods.getGoodsName());
				vendMachineInt.setHotStatus("1");
				vendMachineIntService.insertVendMachineInt(vendMachineInt);
				vendMachineInts.add(vendMachineInt);
			}
		}
		model.addAttribute("id", id);
		model.addAttribute("vendMachineInts", vendMachineInts);
		return "manage/machine/machine_initialize"; 
	}
	/**
	 * 初始化机器信息
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value="/initialize",method=RequestMethod.POST)
	public String initialize(HttpServletRequest request,HttpServletResponse response) throws IOException{
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		String success="0";
		String msg="初始化失败";
		String vendMachineIntArray[]=request.getParameterValues("vendMachineIntArray");
		for(int i=0;i<vendMachineIntArray.length;i++){
			String vendMachineIntJson=vendMachineIntArray[i];
			Map<String,Object> vendMachineIntMap=JsonUtil.getMap4Json(vendMachineIntJson);
			VendMachineInt vendMachineInt=vendMachineIntService.getOne(Function.getInt(vendMachineIntMap.get("id").toString(),0));
		    if(vendMachineInt!=null){
		    	String hot=vendMachineInt.getHotStatus().equals("0")?"冷":"热";
		    	if(vendMachineIntMap.get("waterOutTime").toString().equals("")){
		    		success="0";
		    		msg="商品"+vendMachineInt.getGoodsName()+"（"+hot+"）的出水时间不能为空";
		    		break;
		    	}
		    	if(!StringUtils.isNumeric(vendMachineIntMap.get("waterOutTime").toString())){
		    		success="0";
		    		msg="商品"+vendMachineInt.getGoodsName()+"（"+hot+"）的出水时间只能填数字";
		    		break;
		    	}
		    	if(Function.getInt(vendMachineIntMap.get("waterOutTime").toString(),0)<10&&Function.getInt(vendMachineIntMap.get("waterOutTime").toString(),0)>999){
		    		success="0";
		    		msg="商品"+vendMachineInt.getGoodsName()+"（"+hot+"）的出水时间只能必须在10~999之间";
		    		break;
		    	}
		    	if(vendMachineIntMap.get("grainOutTime").toString().equals("")){
		    		success="0";
		    		msg="商品"+vendMachineInt.getGoodsName()+"（"+hot+"）的出料时间不能为空";
		    		break;
		    	}
		    	if(!StringUtils.isNumeric(vendMachineIntMap.get("grainOutTime").toString())){
		    		success="0";
		    		msg="商品"+vendMachineInt.getGoodsName()+"（"+hot+"）的出料时间只能填数字";
		    		break;
		    	}
		    	if(Function.getInt(vendMachineIntMap.get("grainOutTime").toString(),0)<10&&Function.getInt(vendMachineIntMap.get("waterOutTime").toString(),0)>999){
		    		success="0";
		    		msg="商品"+vendMachineInt.getGoodsName()+"（"+hot+"）的出料时间只能必须在10~999之间";
		    		break;
		    	}
		    	vendMachineInt.setWareName(vendMachineIntMap.get("wareName").toString());
		    	vendMachineInt.setWaterOutTime(Function.getInt(vendMachineIntMap.get("waterOutTime").toString(),0));
		    	vendMachineInt.setGrainOutTime(Function.getInt(vendMachineIntMap.get("grainOutTime").toString(),0));
		    	vendMachineInt.setExtend1("1");
		    	int isOk=vendMachineIntService.editVendMachineInt(vendMachineInt);
		    	if(isOk==1){
		    		success="1";
			    	msg="初始化成功";
		    	}
		    }
		}
		json.put("success", success);
		json.put("msg", msg);
		response.getWriter().append(json.toJSONString());
		return null;
	}
}
