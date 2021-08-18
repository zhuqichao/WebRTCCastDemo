package com.herman.castservice.controller

import com.herman.castservice.bean.DeviceInfo
import com.herman.castservice.model.request.DemoRequest
import com.herman.castservice.model.response.DemoResponse
import com.herman.castservice.model.response.NetResponse
import com.herman.castservice.service.CastService
import com.herman.castservice.service.CastServiceImpl
import org.springframework.web.bind.annotation.*


//@Controller  注册Controller对象
@CrossOrigin // 支持跨域
@RestController // @Controller与 ResponseBody合二为一注解
@RequestMapping(path = ["/cast"]) // 地址值
class CastController {
    private val castService: CastService = CastServiceImpl()

    /**
     * 测试两数相加
     *
     * @return 两数相加结果
     */
    @PostMapping("/testAdd")
    fun testAdd(@RequestBody request: DemoRequest): NetResponse<DemoResponse> {
        return castService.testAdd(request)
    }

    /**
     * 设备信息上报，返回生成的设备短码，即投屏码
     */
    @PostMapping("/initDevice")
    fun initDevice(@RequestBody deviceInfo: DeviceInfo): NetResponse<DeviceInfo> {
        return castService.initDevice(deviceInfo)
    }

    /**
     * 获取设备列表
     */
    @GetMapping("/listDevice")
    fun listDevice(): NetResponse<List<DeviceInfo>> {
        return castService.listDevice()
    }

    /**
     * 检查设备是否存在
     */
    @GetMapping("/checkDevice")
    fun checkDevice(@RequestParam pingCode: String): NetResponse<DeviceInfo> {
        return castService.checkDevice(pingCode)
    }

    @GetMapping("/")
    fun root(): String {
        return "HelloWord"
    }
}