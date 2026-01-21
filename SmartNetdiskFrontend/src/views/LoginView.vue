<template>
  <div class="h-screen flex items-center justify-center bg-[#FAF5FF]">
    <div class="glass-card p-8 w-96">
      <h2 class="text-2xl font-bold text-[#7C3AED] mb-6 text-center">SmartNetdisk</h2>
      <el-form label-position="top">
        <el-form-item label="用户名">
          <el-input placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" class="w-full mt-4" @click="$router.push('/')">登录</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>
