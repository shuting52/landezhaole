package com.landezhaole.app

// 省钱小助手 22 张领券卡（与网站 sq 页同源；短链与二维码渠道原样照搬，见 sq 页说明）
data class CouponCard(
    val tab: Int,
    val platform: String,
    val title: String,
    val desc: String,
    val tags: List<String>,
    val tagClasses: List<String>,
    val url: String
)

val COUPON_TABS = listOf("吃喝餐饮", "旅游出行", "生活服务")
val COUPON_FILTERS = listOf(
    listOf("all" to "全部", "meituan" to "美团", "taobao" to "淘宝闪购", "jingdong" to "京东", "xiawucha" to "下午茶", "xianxia" to "堂食"),
    listOf("all" to "全部", "jipiao" to "机票", "huochepiao" to "火车票", "dache" to "网约车", "jiudian" to "酒店", "danche" to "单车", "daijia" to "代驾", "shunfengche" to "顺风车", "lahuoche" to "货车"),
    listOf("all" to "全部", "liuliangka" to "流量卡", "dianyingpiao" to "电影", "kuaidi" to "快递")
)
val COUPON_CARDS = listOf(
    CouponCard(0, "美团", "外卖节红包", "最高288元红包", listOf("美团", "外卖"), listOf("meituan", "waimai"), "http://dpurl.cn/5ERL3sSz"),
    CouponCard(0, "美团", "大牌饮品券", "奶茶/咖啡/甜点/汉堡都能用", listOf("美团", "外卖", "下午茶"), listOf("meituan", "waimai", "xiawucha"), "http://dpurl.cn/gSbheEVz"),
    CouponCard(0, "美团", "美团买药", "买药先领大额神券", listOf("美团", "外卖"), listOf("meituan", "waimai"), "http://dpurl.cn/qKozVKAz"),
    CouponCard(0, "美团", "时令水果季", "领券满59元减20元", listOf("美团", "外卖"), listOf("meituan", "waimai"), "http://dpurl.cn/BM2pnh8z"),
    CouponCard(0, "美团", "神券包", "9.9元购360元神券", listOf("美团", "外卖"), listOf("meituan", "waimai"), "https://p0.meituan.net/marketingcpsmedia/54ca1fe3fd52375911454a771e5e579320682.jpg"),
    CouponCard(0, "淘宝闪购", "闪购品牌日", "最高22元的可叠加红包", listOf("淘宝闪购", "外卖"), listOf("taobao", "waimai"), "https://kurl04.cn/tHrixf"),
    CouponCard(0, "淘宝闪购", "天天领红包", "津贴叠加红包，下单抽免单", listOf("淘宝闪购", "外卖"), listOf("taobao", "waimai"), "https://u.ele.me/Z2dZLyd8"),
    CouponCard(0, "京东外卖", "惊喜红包", "惊喜大额红包随机领取", listOf("京东外卖", "外卖"), listOf("jingdong", "waimai"), "https://u.jd.com/fDDLk4W"),
    CouponCard(0, "京东外卖", "京东外卖券", "满25元减13元", listOf("京东外卖", "外卖"), listOf("jingdong", "waimai"), "https://u.jd.com/fGDbxls"),
    CouponCard(0, "饮品专场", "奶茶饮品", "喜茶/茶姬/奈雪/百果园5折起", listOf("堂食", "外卖", "下午茶"), listOf("xianxia", "waimai", "xiawucha"), "http://dpurl.cn/ksHYLoOz"),
    CouponCard(0, "咖啡专场", "咖啡专场", "瑞幸/星巴克/库迪5折起", listOf("堂食", "外卖", "下午茶"), listOf("xianxia", "waimai", "xiawucha"), "https://s.t28.cn/X6Dohm2i"),
    CouponCard(0, "汉堡专场", "汉堡西餐专场", "KFC/麦当劳/必胜客/德克士5折起", listOf("堂食", "外卖", "下午茶"), listOf("xianxia", "waimai", "xiawucha"), "https://s.t28.cn/X6Dohm2i"),
    CouponCard(1, "滴滴出行", "滴滴出行", "出行5折起", listOf("打车", "顺风车", "机票", "火车票", "单车", "代驾", "租车", "货车"), listOf("dache", "shunfengche", "jipiao", "huochepiao", "danche", "daijia", "zuche", "lahuoche"), "https://vv.didi.cn/rlqb33L?source_id=38561jutuike123456&ref_from=dunion"),
    CouponCard(1, "花小猪", "花小猪打车", "最高128元券包", listOf("打车"), listOf("dache"), "https://kurl06.cn/tuKlsr"),
    CouponCard(1, "美团", "美团出行", "领100元福利券包", listOf("火车票", "机票"), listOf("huochepiao", "jipiao"), "http://dpurl.cn/VYaJttEz"),
    CouponCard(1, "飞猪", "飞猪旅行", "最高立减75元", listOf("酒店", "火车票", "机票"), listOf("jiudian", "huochepiao", "jipiao"), "https://a.feizhu.com/1KjqBc"),
    CouponCard(1, "飞猪", "飞猪酒店", "星级酒店最高立减300元", listOf("酒店"), listOf("jiudian"), "https://a.feizhu.com/00akoI"),
    CouponCard(1, "同城", "同程民宿", "民宿好房特惠甩卖，3折起", listOf("民宿"), listOf("jiudian"), "https://kurl01.cn/t0ZgGQ"),
    CouponCard(2, "流量卡", "超值流量卡", "电信/联通/移动/广电，大流量才过瘾", listOf("流量卡"), listOf("liuliangka"), "https://ka.huojukj.com/?u=357159"),
    CouponCard(2, "美团", "电影票", "8折起，全国可通用", listOf("电影票"), listOf("dianyingpiao"), "https://kzurl18.cn/tu5Wj9"),
    CouponCard(2, "淘票票", "特价门票", "景点门票活动，官方出票特价优惠", listOf("景点门票"), listOf("dianyingpiao"), "https://kzurl18.cn/tu5UDa"),
    CouponCard(2, "快递", "发快递", "快递、物流、同城都能享优惠", listOf("快递"), listOf("kuaidi"), "https://kurl06.cn/tu5Wz3"),
)
