项目结构:
src/main/java/com/zxw
├── common        通用响应、异常、安全上下文
├── config        Spring Boot 配置
├── modules
│   ├── auth      登录、注册、验证码
│   ├── apikey    API Key 管理
│   ├── gateway   网关转发、模型调用、扣费日志
│   ├── model     模型与路由管理
│   ├── provider  供应商渠道管理
│   ├── access    套餐、额度、余额
│   ├── request   请求日志
│   └── dashboard 仪表盘统计
└── persistence   实体、Mapper、查询视图
