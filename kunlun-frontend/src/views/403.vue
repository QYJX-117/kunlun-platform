<template>
  <div class="forbidden">
    <div class="code">403</div>
    <div class="text">抱歉，您没有权限访问此页面</div>
    <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
  </div>
</template>

<style scoped>
.forbidden {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  gap: 16px;
}

.code {
  font-size: 72px;
  font-weight: bold;
  color: #E6A23C;
}

.text {
  font-size: 18px;
  color: #909399;
}
</style>
