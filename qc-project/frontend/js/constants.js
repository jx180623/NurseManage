/**
 * constants.js — 质控类别、条目定义（与后端 FREE_INPUT_KEYS 完全对应）
 * 每次前端 CATS 变更，后端 ReportServiceImpl.FREE_INPUT_KEYS 必须同步更新
 */

export const CATEGORIES = [
  {
    id: 'basic', name: '基础护理',
    items: [
      '科室患者基础护理质量（三短六洁、床单位情况）',
      '护理员能说出重点患者的风险因素：一级、压疮、高跌、管路、皮肤'
    ]
  },
  {
    id: 'safety', name: '安全',
    items: [
      '科室患者口服给药落实情况',
      '科室患者治疗卡执行完成并签名',
      '科室抢救车完好率',
      '新入院/转入患者腕带正确佩戴，必要时重新打印，有护理记录'
    ]
  },
  {
    id: 'quality', name: '优质',
    items: [
      '科室患者ADL与护理级别匹配',
      '科室责负责患者均有药单并宣教内容正确',
      '科室患者已完成清单查询方式宣教',
      '当日科室新入院患者已完成杏一或便利店物品购买方式宣教',
      '午餐品质询问患者就餐满意情况（每日填1个患者，床号 姓名 反馈情况）',
      '压疮/高风险压疮患者皮肤护理及时率'
    ]
  },
  { id: 'discipline', name: '纪律', items: ['护士到岗情况'] },
  {
    id: 'tube', name: '管路',
    items: [
      '科室患者三管评估已完成',
      '科室新建管路患者已完成"非计划拔管"系统填写'
    ]
  },
  { id: 'sevens', name: '7S', items: ['科室负责 7S 分担区已完成整理'] },
  {
    id: 'fall', name: '防跌措施落实情况',
    items: [
      '科室患者胃肠镜相关跌倒风险评估已落实',       // fall_0  多选
      '科室高跌患者危险因素已与护理员完成交接',       // fall_1  多选
      '科室高跌患者知晓起床三部曲等防跌知识',         // fall_4  多选
      '科室高跌患者知晓呼叫器使用方法（床头及卫生间，并且呼叫器在便于拿取的地方）', // fall_5 多选
      '科室头晕/黑蒙的患者知晓防跌等处理措施',       // fall_6  多选
      '科室高跌患者拖鞋合格',                         // fall_7  多选
      '护理员能说出分管高跌患者及风险因素',           // fall_8  手工输入
      '科室高跌患者均有红色腕带（尤其关注转入和术后）', // fall_9  多选
      '严重血管病变的患者完成如何正确跌倒宣教'

    ]
  },
  {
    id: 'specialty', name: '专科指标',
    items: [
      '责负责心衰患者有体重监测（入组患者标准见备注）',
      '责组完成患者吞咽能力评估（入组患者标准见备注）'
    ]
  }
];

/**
 * 被质控者姓名使用「手工文本输入」的条目 key 集合
 * 与后端 ReportServiceImpl.FREE_INPUT_KEYS 完全对应
 */
export const FREE_INPUT_KEYS = new Set([
  'basic_0',  // 基础护理 - 护理质量
  'basic_1',  // 基础护理 - 护理员知晓风险
  'fall_8',   // 防跌 - 护理员知晓高跌患者
]);

/**
 * 出入院字段定义（前端渲染顺序）
 * calcToday=true 表示该字段参与今日总人数自动计算
 * calcRate=true  表示该字段参与陪护率计算
 */
export const ADM_FIELDS = [
  { id: 'yesterdayTotal', label: '昨日患者总人数', calcToday: true },
  { id: 'in',             label: '入院人数',        calcToday: true },
  { id: 'transferIn',     label: '转入人数',         calcToday: true },
  { id: 'out',            label: '出院人数',         calcToday: true },
  { id: 'transferOut',    label: '转出人数',         calcToday: true },
  { id: 'er',             label: '急诊转入人数' },
  { id: 'surgery',        label: '手术人数' },
  { id: 'level1',         label: '一级人数' },
  { id: 'level4',         label: '四级手术人数' },
  { id: 'escort',         label: '陪护人数',         calcRate: true },
];
