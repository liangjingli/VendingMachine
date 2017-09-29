package vend.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;

import base.util.DateUtil;
import base.util.EncodeUtils;
import base.util.Function;
import vend.entity.CodeLibrary;
import vend.entity.VendGoods;
import vend.entity.VendMachine;
import vend.entity.VendOrder;
import vend.entity.VendQrcodeAttend;
import vend.entity.VendUser;
import vend.service.CodeLibraryService;
import vend.service.GetWeiXinUserInfoService;
import vend.service.VendGoodsService;
import vend.service.VendMachineService;
import vend.service.VendOrderService;
import vend.service.VendQrcodeAttendService;
import vend.service.VendRoleService;
import vend.service.VendSyslogService;
import vend.service.VendUserService;

@Controller
@RequestMapping("/mobile")
public class MobileController {
	public static Logger logger = Logger.getLogger(MobileController.class);
	@Autowired
	VendUserService vendUserService;
	@Autowired
	VendSyslogService vendSyslogService;
	@Autowired
	VendRoleService vendRoleService;
	@Autowired
	VendGoodsService vendGoodsService;
	@Autowired
	VendMachineService vendMachineService;
	@Autowired
	CodeLibraryService codeLibraryService;
	@Autowired
	VendOrderService vendOrderService;
	@Autowired
	VendQrcodeAttendService vendQrcodeAttendService;
	@Autowired
	GetWeiXinUserInfoService getWeiXinUserInfoService;
	
	/**
	 * 获取微信公众号关注用户的code
	 * @param code
	 * @param state
	 * @param request
	 * @param model
	 * @return
	 */
	@RequestMapping(value="/getcode")
	public String getCode(HttpServletRequest request,Model model){
		String code=request.getParameter("code");
		String state=request.getParameter("state");
		logger.info("---------微信公众号关注用户的code:----------"+code);
		logger.info("---------微信公众号关注用户的state:----------"+state);
		HttpSession session=request.getSession();
		String wechatpubNo=(String)session.getAttribute("wechatpubNo");
		logger.info("---------2:微信公众号的为微信号:----------"+wechatpubNo);
		Map<String,String> map1=getWeiXinUserInfoService.getAccessToken(wechatpubNo, code);//获取微信用户的access_token和openid
		logger.info("---------map1是否为空----------"+map1.isEmpty());
		VendUser ShVendUser=vendUserService.selectByWechatpubNo(wechatpubNo);//商户信息
		if(ShVendUser==null){
			logger.info("---------ShVendUser为空----------");
		}
		
		if(ShVendUser!=null){
			if(!map1.isEmpty()){
				String access_token = map1.get("access_token");
				String openid = map1.get("openid");
				logger.info("---------mobile/getcode:微信公众号关注用户的access_token:----------"+access_token);
				logger.info("---------mobile/getcode:微信公众号关注用户的openid:----------"+openid);
				
				Map<String,String> map2=getWeiXinUserInfoService.getWxUserInfo(wechatpubNo, access_token, openid);
				logger.info("---------map2是否为空----------"+map1.isEmpty());
				if(!map2.isEmpty()){
					String nickname = map2.get("nickname");
					String city = map2.get("city");
					logger.info("---------mobile/getcode:微信公众号关注用户的nickname:----------"+nickname);
					logger.info("---------mobile/getcode:微信公众号关注用户的city:----------"+city);
					
					VendUser vendUser =vendUserService.selectByUsername(nickname);
					logger.info("---------mobile/getcode:vendUser用户信息nickname:----------"+vendUser);
					Date createTime=DateUtil.parseDateTime(DateUtil.getCurrentDateTimeStr());
					logger.info("---------mobile/getcode:vendUser用户信息createTime:----------"+createTime);
					logger.info("---------mobile/getcode:vendUser用户信息vendUser:----------"+vendUser);
					if(vendUser==null){
						logger.info("---------11:添加用户信息:----------");
						vendUser=new VendUser();
						String usercode=Function.getUsercode();
						vendUser.setUsercode(usercode);
						vendUser.setUsername(nickname);
						vendUser.setAddress(city);
						String password=Function.getEncrypt("123456");
						vendUser.setPassword(password);
						vendUser.setRoleId(5);
						int isOk=vendUserService.insertVendUser(vendUser);
						logger.info("---------11:添加用户信息isOk:----------"+isOk);
						if(isOk==1){
							model.addAttribute("usercode", usercode);
						}
						logger.info("---------12:添加用户信息:----------");
						//二维码关注操作
						logger.info("---------11:添加用户关注商户二维码信息:----------");
						VendQrcodeAttend vendQrcodeAttend=new VendQrcodeAttend();
						vendQrcodeAttend.setAttendTime(createTime);
						vendQrcodeAttend.setUsercode(vendUser.getUsercode());
						vendQrcodeAttend.setExtend1(ShVendUser.getUsercode());
						vendQrcodeAttendService.insertVendQrcodeAttend(vendQrcodeAttend);
						logger.info("---------12:添加用户关注商户二维码信息:----------");
					}else{
						logger.info("---------21:添加用户关注商户二维码信息:----------");
						logger.info("---------22:添加用户关注商户二维码信息usercode:----------"+vendUser.getUsercode());
						model.addAttribute("usercode", vendUser.getUsercode());
						//二维码关注操作
						VendQrcodeAttend vendQrcodeAttend=new VendQrcodeAttend();
						logger.info("---------23:添加用户关注商户二维码信息createTime:----------"+createTime);
						vendQrcodeAttend.setAttendTime(createTime);
						vendQrcodeAttend.setUsercode(vendUser.getUsercode());
						logger.info("---------24:添加用户关注商户二维码信息Shusercode:----------"+ShVendUser.getUsercode());
						vendQrcodeAttend.setExtend1(ShVendUser.getUsercode());
						int isOk=vendQrcodeAttendService.insertVendQrcodeAttend(vendQrcodeAttend);
						logger.info("---------24:添加用户关注商户二维码信息是否成功:----------"+isOk);
						logger.info("---------25:添加用户关注商户二维码信息:----------");
					}
				}
			}
		}
		
		logger.info("---------开始返回:----------");
		List<VendGoods> vendGoodss=vendGoodsService.findAll();
    	model.addAttribute("vendGoodss", vendGoodss);
		return "../../mobile/goods_list";
	}
	/**
	 * 消费用户手机登录
	 * @param wechatpubNo
	 */
    @RequestMapping(value="/mobilelogin",method=RequestMethod.GET)
    @ResponseBody
    public void mobileLogin(@RequestParam String wechatpubNo){
    	logger.info("---------1:微信公众号的为微信号:----------"+wechatpubNo);
    }
    /**
     * 微信公众号关注用户跳转页面
     * @param model
     * @param request
     * @param wechatpubNo
     * @throws Exception
     */
    @RequestMapping(value="/{wechatpubNo}/login",method=RequestMethod.GET)
    @ResponseBody
	public 	String login(Model model, HttpServletRequest request,HttpServletResponse response,@PathVariable String wechatpubNo) throws Exception{
    	CodeLibrary codeLibrary=codeLibraryService.selectByItemno("WECHATPUBNO", wechatpubNo);
    	if(codeLibrary==null){
    		logger.info("该公众号未绑定自助饮品系统");
    		return null;
    	}
    	if(codeLibrary.getItemname()==null){
    		logger.info("该公众号开发者ID未填写");
    		return null;
    	}
    	if(codeLibrary.getExtend1()==null){
    		logger.info("该公众号开发者密钥未填写");
    		return null;
    	}
    	
    	String url="";
		String redirect_uri=EncodeUtils.urlEncode("http://zdypx.benbenniaokeji.com/mobile/getcode");
		if(codeLibrary!=null){
			url="https://open.weixin.qq.com/connect/oauth2/authorize?appid="+codeLibrary.getItemname()+""
					+ "&redirect_uri="+redirect_uri+"&response_type=code&scope=snsapi_userinfo&state=123#wechat_redirect";
		}
    	
    	HttpSession session=request.getSession();
    	session.setAttribute("wechatpubNo", wechatpubNo);
    	
    	response.setCharacterEncoding("UTF-8");
    	response.getWriter().print("<script>window.location.href='"+url+"'</script>");
    	return null;
		//return "../../mobile/goods_list";
	}
    /**
     * 免费商品列表
     * @param model
     * @param wechatpubNo
     * @return
     */
    @RequestMapping(value="/goodss",method=RequestMethod.GET)
    public String freeGoodss(Model model){
    	List<VendGoods> vendGoodss=vendGoodsService.findAll();
    	//model.addAttribute("wechatpubNo", wechatpubNo);
    	model.addAttribute("vendGoodss", vendGoodss);
    	return "../../mobile/goods_list";
    }
    /**
     * 跳转商品详情
     * @param model
     * @return
     */
    @RequestMapping(value="/{id}/{usercode}/detail",method=RequestMethod.GET)
    public String goodsDetail(Model model,@PathVariable int id,@PathVariable String usercode){
    	VendGoods vendGoods=vendGoodsService.getOne(id);
    	model.addAttribute("usercode", usercode);
    	model.addAttribute("vendGoods", vendGoods);
    	return "../../mobile/goods_detail";
    }
    /**
     * 免费获取商品
     * @param response
     * @param map
     * @return
     * @throws IOException
     */
	@RequestMapping(value="/freePay",method=RequestMethod.POST)
	public String freePay(HttpServletResponse response,@RequestBody Map<String, String> map) throws IOException{
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		json.put("success", "0");
		json.put("msg", "购买失败");
		
		int id=Function.getInt(map.get("id"),0);//商品ID
		String machinecode=map.get("machinecode");//机器码
		
		VendMachine vendMachine=vendMachineService.selectByMachineCode(machinecode);
		String shopusercode="";//购买的商品所属的商家
		if(vendMachine!=null){
			shopusercode=vendMachine.getUsercode();
		}
		String usercode=map.get("usercode");
		//1,订单操作
		VendOrder vendOrder=new VendOrder();
		/**售卖指令*/
		if(vendOrder.getMachineCode()!=null){
			VendGoods vendGoods=vendGoodsService.getOne(vendOrder.getGoodsId());
			if(vendMachine!=null&&vendGoods!=null){
				vendGoodsService.sellGoods(vendMachine, vendGoods, vendOrder);
			}
		}
		/**售卖指令*/
		
		Date createTime=DateUtil.parseDateTime(DateUtil.getCurrentDateTimeStr());
		vendOrder.setAmount(BigDecimal.valueOf(0.00));
		String orderId=Function.getOrderId();
		vendOrder.setOrderId(orderId);
		vendOrder.setUsercode(usercode);
		vendOrder.setShopusercode(shopusercode);
		vendOrder.setGoodsId(id);
		vendOrder.setMachineCode(machinecode);
		vendOrder.setNum(1);
		vendOrder.setCreateTime(createTime);
		vendOrder.setOrderstate("1");
		vendOrder.setFreeStatus("1");//2优惠券代替支付免费，1关注二维码免费，0不免费支付
		vendOrder.setExtend1("1");//购买
		vendOrder.setPayType("扫描二维码免费领取");
		int isOk=vendOrderService.insertVendOrder(vendOrder);
		if(isOk==1){
			json.put("success", 1);
			json.put("msg", "购买成功");
		}
		try {
			response.getWriter().append(json.toJSONString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
