package com.lwb.guahao.webapp.controller.my_hospital;

import com.lwb.guahao.common.ApiRet;
import com.lwb.guahao.common.Paging;
import com.lwb.guahao.common.option.OptionMap;
import com.lwb.guahao.common.util.lang.DateUtils;
import com.lwb.guahao.common.option.util.DeptClassUtil;
import com.lwb.guahao.common.util.lang.DoubleUtils;
import com.lwb.guahao.common.util.lang.IntegerUtils;
import com.lwb.guahao.common.model.Doctor;
import com.lwb.guahao.common.model.DoctorPerTimeSchedule;
import com.lwb.guahao.common.qo.DoctorDailyScheduleQo;
import com.lwb.guahao.webapp.component.PagingComponent;
import com.lwb.guahao.webapp.service.DoctorPerTimeScheduleService;
import com.lwb.guahao.webapp.service.DoctorService;
import com.lwb.guahao.webapp.service.HospitalService;
import com.lwb.guahao.webapp.service.LoginService;
import com.lwb.guahao.webapp.vo.*;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: Lu Weibiao
 * Date: 2015/2/28 22:10
 */
@Controller
@RequestMapping(value = "/myHospital")
public class MyHospitalController {
    private static final Logger logger = Logger.getLogger(MyHospitalController.class);
    @Resource
    private LoginService loginService;
    @Resource
    private HospitalService hospitalService;
    @Resource
    private DoctorService doctorService;
    @Resource
    private DoctorPerTimeScheduleService doctorPerTimeScheduleService;
    @Resource
    private PagingComponent pagingComponent;

    @RequestMapping(value = "index")
    public String index(HttpServletRequest request, Model model) {
        return "/my_hospital/index";
    }

    @RequestMapping(value = "baseInfo")
    public String baseInfo(HttpServletRequest request, Model model) {
        HospitalVo loginedHospital = loginService.getLoginedHospital(request);
        model.addAttribute("hospital", loginedHospital);
        return "/inc/my_hospital/baseInfo";
    }

    @RequestMapping(value = "doctors")
    public String doctors(HttpServletRequest request, Model model,
                          String name,
                          @RequestParam(required = false) Integer deptClassCode,
                          String accountName,
                          @RequestParam(required = false) Integer pn) {
        Integer curHospitalId = loginService.getLoginedHospitalId(request);
        Paging<DoctorVo> doctorPaging = hospitalService.getDoctorPaging(curHospitalId, name, deptClassCode, accountName, pn);
        String queryStringWithoutPn = pagingComponent.getQueryStringWithoutPn(request);
        model.addAttribute("doctorPaging", doctorPaging);
        model.addAttribute("deptClassList", DeptClassUtil.deptClassList);
        model.addAttribute("queryStringWithoutPn", queryStringWithoutPn);
        model.addAttribute("name", name);
        model.addAttribute("deptClassCode", deptClassCode);
        model.addAttribute("deptClassName", OptionMap.deptClassMap.get(deptClassCode));
        model.addAttribute("accountName", accountName);
        return "/inc/my_hospital/doctors";
    }

    /**
     * 医生账号创建页面
     *
     * @param request
     * @param model
     * @return
     */
    @RequestMapping(value = "doctor/empty")
    public String emptyDoctor(HttpServletRequest request, Model model) {
        return "/inc/my_hospital/emptyDoctor";
    }

    /**
     * 创建医生账号
     *
     * @param request
     * @param model
     * @param doctor
     */
    @RequestMapping(value = "doctor/create", method = RequestMethod.POST)
    public void createDoctor(HttpServletRequest request, Model model, Doctor doctor) {
        ApiRet apiRet = new ApiRet();
        if (StringUtils.isEmpty(doctor.getAccountName())) {
            apiRet.setRet(ApiRet.RET_FAIL);
            apiRet.setMsg("账号名不能为空");
        } else if (StringUtils.isEmpty(doctor.getPassword())) {
            apiRet.setRet(ApiRet.RET_FAIL);
            apiRet.setMsg("密码不能为空");
        } else if (StringUtils.isEmpty(doctor.getName())) {
            apiRet.setRet(ApiRet.RET_FAIL);
            apiRet.setMsg("医生名称不能为空");
        } else if (doctor.getDeptClassCode() == null) {
            apiRet.setRet(ApiRet.RET_FAIL);
            apiRet.setMsg("科室类目不能为空");
        } else if (StringUtils.isEmpty(doctor.getSex())) {
            apiRet.setRet(ApiRet.RET_FAIL);
            apiRet.setMsg("医生性别不能为空");
        }
        if (apiRet.getRet() != ApiRet.RET_FAIL) {
            Integer curHospitalId = loginService.getLoginedHospitalId(request);
            doctor.setHospitalId(curHospitalId);
            apiRet = hospitalService.createDoctor(doctor);
        }
        model.addAttribute("ret", apiRet.getRet());
        model.addAttribute("msg", apiRet.getMsg());
    }

    /**
     * 获取某个医生的排班列表（分页）
     *
     * @param request
     * @param model
     * @param doctorIdStr
     * @param qoVo
     * @return
     */
    @RequestMapping(value = "doctor/{doctorIdStr}/dailySchedules", method = RequestMethod.GET)
    public String doctorDailySchedules(HttpServletRequest request, Model model, @PathVariable String doctorIdStr, DoctorDailyScheduleQoVo qoVo) throws ParseException{
        Integer curHospitalId = loginService.getLoginedHospitalId(request);
        Integer doctorId = Integer.valueOf(doctorIdStr);
        ApiRet apiRet = new ApiRet();
        String view = null;
        if (!hospitalService.hasThisDoctor(curHospitalId, doctorId)) {
            apiRet.setRet(ApiRet.RET_FAIL);
            apiRet.setMsg("没有访问权限");
            view = null;
        } else {
            DoctorVo doctorVo = doctorService.getDoctor(doctorId);

            qoVo.setDoctorId(doctorIdStr);
            DoctorDailyScheduleQo qo = DoctorDailyScheduleQo.parse(qoVo);

            apiRet = doctorPerTimeScheduleService.getPagingBy(qo);

            model.addAttribute("doctor",doctorVo);
            model.addAttribute("doctorDailyScheduleQo", qoVo);
            model.addAttribute("doctorDailySchedulePaging", (Paging<DoctorDailyScheduleVo>) apiRet.getData());
            model.addAttribute("queryStringWithoutPn", pagingComponent.getQueryStringWithoutPn(request));
            view = "/inc/my_hospital/doctor/dailySchedules";
        }
        model.addAttribute("ret", apiRet.getRet());
        model.addAttribute("msg", apiRet.getMsg());
        return view;
//        return "/inc/my_hospital/doctor/dailySchedules";
    }

    /**
     * 保存修改指定医生的某天排班
     *
     * @param request
     * @param model
     * @return
     */
    @RequestMapping(value = "dailySchedule/saveOrUpdate")
    public void doctorDailyScheduleSaveOrUpdate(HttpServletRequest request, Model model) {
        ApiRet apiRet = new ApiRet();
        try {
            /*根据传入参数构建doctorPerTimeSchedule List*/
            String scheduleDay = request.getParameter("scheduleDay");
            String[] doctorPerTimeScheduleIdArr = request.getParameterValues("doctorPerTimeScheduleId");
            String[] startTimeArr = request.getParameterValues("startTime");
            String[] endTimeArr = request.getParameterValues("endTime");
            String[] totalSourceArr = request.getParameterValues("totalSource");
            String priceStr = request.getParameter("price");
            Double price = DoubleUtils.parseString(priceStr, 0.0);
            String doctorId = request.getParameter("doctorId");
            int size = doctorPerTimeScheduleIdArr.length;
            List<DoctorPerTimeSchedule> doctorPerTimeScheduleList = new ArrayList<DoctorPerTimeSchedule>(size);
            for (int i = 0; i < size; i++) {
                if (StringUtils.isEmpty(startTimeArr[i]) || StringUtils.isEmpty(endTimeArr[i])) {
                    continue;
                }
                DoctorPerTimeSchedule schedule = new DoctorPerTimeSchedule();
                schedule.setId(IntegerUtils.parseString(doctorPerTimeScheduleIdArr[i], null));
                schedule.setDoctorId(Integer.valueOf(doctorId));
                Date startDateTime = DateUtils.yearMonthDayWeekTimeFormatter.parse(scheduleDay + " " + startTimeArr[i]);
                schedule.setStartDateTime(startDateTime);
                Date endDateTime = DateUtils.yearMonthDayWeekTimeFormatter.parse(scheduleDay + " " + endTimeArr[i]);
                schedule.setEndDateTime(endDateTime);
                schedule.setTotalSource(IntegerUtils.parseString(totalSourceArr[i], 0));
                schedule.setOddSource(schedule.getTotalSource());
                schedule.setPrice(price);
                doctorPerTimeScheduleList.add(schedule);
            }

            apiRet = doctorPerTimeScheduleService.saveOrUpdate(doctorPerTimeScheduleList);

        } catch (Exception e) {
            logger.error("保存失败",e);
            apiRet.setRet(ApiRet.RET_FAIL);
            apiRet.setMsg("保存失败");
        }

        model.addAttribute("ret", apiRet.getRet());
        model.addAttribute("msg", apiRet.getMsg());
    }

    /**
     * 删除当前医院账号下指定的排班
     * @param request
     * @param model
     * @param doctorPerTimeScheduleId
     */
    @RequestMapping(value = "doctorPerTimeSchedule/{doctorPerTimeScheduleId}/del")
    public void deleteDoctorPerTimeSchedule(HttpServletRequest request, Model model, @PathVariable Integer doctorPerTimeScheduleId) {
        ApiRet apiRet = new ApiRet(ApiRet.RET_SUCCESS, "删除成功", null);
        Integer hospitalId = loginService.getLoginedHospitalId(request);

        try {
            if (!hospitalService.hasThisPerTimeSchedule(hospitalId, doctorPerTimeScheduleId)) {
                apiRet.setRet(ApiRet.RET_FAIL);
                apiRet.setMsg("没有删除权限");
            } else{
                doctorPerTimeScheduleService.delete(doctorPerTimeScheduleId);
            }
        } catch (Exception e) {
            apiRet.setRet(ApiRet.RET_FAIL);
            apiRet.setMsg("删除失败");
            logger.error(e);
        }

        model.addAttribute("ret", apiRet.getRet());
        model.addAttribute("msg", apiRet.getMsg());
    }
}
