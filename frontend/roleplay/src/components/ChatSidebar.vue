<template>
  <div class="w-full h-full bg-white/80 backdrop-blur-xl flex flex-col p-4 gap-4 border-r border-white/40 shadow-[4px_0_24px_rgba(0,0,0,0.02)]">
    <!-- 用户模块 -->
    <UserModule />
    
    <!-- 滚动容器 -->
    <div class="flex-1 flex flex-col min-h-0 overflow-y-auto space-y-4 custom-scrollbar">
      <!-- Qwen Assistant 区域 -->
      <QwenSection />
      
      <!-- 分隔线 -->
      <div class="relative">
        <div class="absolute inset-0 flex items-center" aria-hidden="true">
          <div class="w-full border-t border-gray-200"></div>
        </div>
      </div>
      
      <!-- AI Companions 区域 -->
      <CompanionsSection />
    </div>
  </div>
</template>

<script setup lang="ts">
import UserModule from './UserModule.vue'
import QwenSection from './QwenSection.vue'
import CompanionsSection from './CompanionsSection.vue'
</script>

<style scoped>
/* 隐藏滚动条但保留滚动功能 */
.custom-scrollbar {
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE 10+ */
}

.custom-scrollbar::-webkit-scrollbar {
  display: none; /* Chrome/Safari/Webkit */
}
</style>
