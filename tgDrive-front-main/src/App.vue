<template>
  <router-view />
</template>

<script setup lang="ts">
// No script logic needed here, it's handled by layouts.
</script>

<style>
/* 全局样式重置和基础设置 */
* {
  box-sizing: border-box;
}

html, body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  font-weight: 500;
}

#app {
  height: 100vh;
  width: 100vw;
  overflow-x: hidden; /* Prevent horizontal scrolling */
  overflow-y: auto;   /* Allow vertical scrolling when needed */
  border-radius: 0 !important; /* 移除应用整体圆角 */
}

/* 全局圆角样式 - 更圆润版本 */
/* Element Plus 组件圆角 */
.el-card {
  border-radius: 20px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.el-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.el-button {
  border-radius: 16px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.el-button:active {
  transform: translateY(0);
  transition: all 0.1s;
}

.el-button::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.el-button:active::before {
  width: 300px;
  height: 300px;
}

.el-input .el-input__wrapper {
  border-radius: 16px !important;
  transition: all 0.3s ease;
}

.el-input .el-input__wrapper:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.el-input .el-input__wrapper.is-focus {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.el-select .el-input .el-input__wrapper {
  border-radius: 16px !important;
  transition: all 0.3s ease;
}

.el-textarea .el-textarea__inner {
  border-radius: 16px !important;
  transition: all 0.3s ease;
}

.el-dialog {
  border-radius: 24px !important;
  animation: dialogFadeIn 0.3s ease-out;
}

@keyframes dialogFadeIn {
  from {
    opacity: 0;
    transform: scale(0.8) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.el-message-box {
  border-radius: 24px !important;
  animation: messageBoxSlideIn 0.3s ease-out;
}

@keyframes messageBoxSlideIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.el-drawer {
  border-radius: 24px 0 0 24px !important;
}

.el-dropdown-menu {
  border-radius: 16px !important;
  animation: dropdownSlideDown 0.2s ease-out;
}

@keyframes dropdownSlideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.el-menu {
  border-radius: 16px !important;
}

.el-menu-item {
  border-radius: 12px !important;
  margin: 2px 4px !important;
  transition: all 0.3s ease;
}

.el-menu-item:hover {
  transform: translateX(4px);
  background-color: var(--el-menu-hover-bg-color) !important;
}

.el-table {
  border-radius: 16px !important;
  overflow: hidden;
}

.el-table th {
  border-radius: 0 !important;
}

.el-table tbody tr {
  transition: all 0.3s ease;
}

.el-table tbody tr:hover {
  transform: scale(1.01);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.el-table td {
  border-radius: 0 !important;
}

.el-pagination {
  border-radius: 16px !important;
}

.el-pagination .el-pager li {
  border-radius: 12px !important;
}

.el-pagination .btn-prev,
.el-pagination .btn-next {
  border-radius: 12px !important;
}

.el-tag {
  border-radius: 12px !important;
}

.el-alert {
  border-radius: 16px !important;
}

.el-notification {
  border-radius: 20px !important;
}

.el-message {
  border-radius: 16px !important;
}

.el-tooltip__popper {
  border-radius: 12px !important;
}

.el-popover {
  border-radius: 16px !important;
}

.el-form-item {
  border-radius: 12px !important;
}

.el-upload {
  border-radius: 16px !important;
}

.el-upload-dragger {
  border-radius: 16px !important;
}

.el-progress-bar__outer {
  border-radius: 20px !important;
}

.el-progress-bar__inner {
  border-radius: 20px !important;
}

.el-switch {
  border-radius: 20px !important;
}

.el-checkbox {
  border-radius: 8px !important;
}

.el-radio {
  border-radius: 50% !important;
}

.el-avatar {
  border-radius: 50% !important;
}

.el-image {
  border-radius: 16px !important;
}

/* 自定义组件圆角 */
.page-container {
  border-radius: 20px !important;
}

.content-card {
  border-radius: 20px !important;
}

.stat-card {
  border-radius: 18px !important;
}

.author-card {
  border-radius: 20px !important;
}

.file-card {
  border-radius: 18px !important;
}

.user-avatar {
  border-radius: 50% !important;
}

/* 容器和布局圆角 - 移除边缘圆角 */
.el-container {
  border-radius: 0 !important;
}

.el-header {
  border-radius: 0 !important;
}

.el-aside {
  border-radius: 0 !important;
}

.el-main {
  border-radius: 0 !important;
}

.el-footer {
  border-radius: 0 !important;
}

/* 响应式圆角调整 */
@media (max-width: 768px) {
  .el-card {
    border-radius: 16px !important;
  }
  
  .el-dialog {
    border-radius: 16px !important;
  }
  
  .el-container {
    border-radius: 0 !important;
  }
}

/* 全局响应式断点 */
/* 超小屏幕 (手机, 小于576px) */
@media (max-width: 575.98px) {
  .container {
    padding: 0 10px;
  }
  
  .el-card {
    margin: 10px 0;
    border-radius: 8px;
  }
  
  .el-button {
    font-size: 14px;
    padding: 8px 12px;
  }
}

/* 小屏幕 (平板, 576px 及以上) */
@media (min-width: 576px) and (max-width: 767.98px) {
  .container {
    padding: 0 15px;
  }
}

/* 中等屏幕 (桌面显示器, 768px 及以上) */
@media (min-width: 768px) and (max-width: 991.98px) {
  .container {
    padding: 0 20px;
  }
}

/* 大屏幕 (大桌面显示器, 992px 及以上) */
@media (min-width: 992px) and (max-width: 1199.98px) {
  .container {
    padding: 0 25px;
  }
}

/* 超大屏幕 (超大桌面显示器, 1200px 及以上) */
@media (min-width: 1200px) {
  .container {
    padding: 0 30px;
    max-width: 1200px;
    margin: 0 auto;
  }
}

/* 通用响应式工具类 */
.text-center {
  text-align: center;
}

.w-100 {
  width: 100%;
}

.d-flex {
  display: flex;
}

.justify-content-center {
  justify-content: center;
}

.align-items-center {
  align-items: center;
}

.flex-column {
  flex-direction: column;
}

.flex-wrap {
  flex-wrap: wrap;
}

.gap-1 {
  gap: 8px;
}

.gap-2 {
  gap: 16px;
}

.gap-3 {
  gap: 24px;
}

/* 响应式显示/隐藏类 */
@media (max-width: 575.98px) {
  .hidden-xs {
    display: none !important;
  }
}

@media (min-width: 576px) {
  .hidden-sm-and-up {
    display: none !important;
  }
}

@media (max-width: 767.98px) {
  .hidden-sm-and-down {
    display: none !important;
  }
}

@media (min-width: 768px) {
  .hidden-md-and-up {
    display: none !important;
  }
}

/* 响应式间距 */
@media (max-width: 767px) {
  .responsive-padding {
    padding: 10px !important;
  }
  
  .responsive-margin {
    margin: 10px !important;
  }
}

@media (min-width: 768px) {
  .responsive-padding {
    padding: 20px !important;
  }
  
  .responsive-margin {
    margin: 20px !important;
  }
}

/* ========== 高级动画效果 ========== */

/* 页面切换动画 */
.router-view {
  animation: pageSlideIn 0.4s ease-out;
}

@keyframes pageSlideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 加载动画 */
.loading-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #409EFF;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 脉冲动画 */
.pulse {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}

/* 弹跳动画 */
.bounce {
  animation: bounce 1s infinite;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

/* 淡入动画 */
.fade-in {
  animation: fadeIn 0.5s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 滑入动画 */
.slide-in-left {
  animation: slideInLeft 0.5s ease-out;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-50px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.slide-in-right {
  animation: slideInRight 0.5s ease-out;
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(50px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.slide-in-up {
  animation: slideInUp 0.5s ease-out;
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(50px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 缩放动画 */
.scale-in {
  animation: scaleIn 0.3s ease-out;
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 旋转动画 */
.rotate {
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 摇摆动画 */
.wobble {
  animation: wobble 1s ease-in-out;
}

@keyframes wobble {
  0% {
    transform: translateX(0%);
  }
  15% {
    transform: translateX(-25px) rotate(-5deg);
  }
  30% {
    transform: translateX(20px) rotate(3deg);
  }
  45% {
    transform: translateX(-15px) rotate(-3deg);
  }
  60% {
    transform: translateX(10px) rotate(2deg);
  }
  75% {
    transform: translateX(-5px) rotate(-1deg);
  }
  100% {
    transform: translateX(0%);
  }
}

/* 闪烁动画 */
.flash {
  animation: flash 1s ease-in-out;
}

@keyframes flash {
  0%, 50%, 100% {
    opacity: 1;
  }
  25%, 75% {
    opacity: 0;
  }
}

/* 渐变背景动画 */
.gradient-bg {
  background: linear-gradient(-45deg, #ee7752, #e73c7e, #23a6d5, #23d5ab);
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
}

@keyframes gradientShift {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

/* 悬浮效果 */
.float {
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-10px);
  }
}

/* 打字机效果 */
.typewriter {
  overflow: hidden;
  border-right: 2px solid #409EFF;
  white-space: nowrap;
  animation: typing 3.5s steps(40, end), blink-caret 0.75s step-end infinite;
}

@keyframes typing {
  from {
    width: 0;
  }
  to {
    width: 100%;
  }
}

@keyframes blink-caret {
  from, to {
    border-color: transparent;
  }
  50% {
    border-color: #409EFF;
  }
}

/* 滚动触发动画 */
.scroll-reveal {
  opacity: 0;
  transform: translateY(50px);
  transition: all 0.6s ease-out;
}

.scroll-reveal.revealed {
  opacity: 1;
  transform: translateY(0);
}

/* 悬停发光效果 */
.glow-on-hover {
  transition: all 0.3s ease;
}

.glow-on-hover:hover {
  box-shadow: 0 0 20px rgba(64, 158, 255, 0.6);
  transform: scale(1.02);
}

/* 波纹效果 */
.ripple {
  position: relative;
  overflow: hidden;
}

.ripple::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.ripple:active::before {
  width: 300px;
  height: 300px;
}

/* 卡片翻转效果 */
.flip-card {
  background-color: transparent;
  perspective: 1000px;
}

.flip-card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  text-align: center;
  transition: transform 0.6s;
  transform-style: preserve-3d;
}

.flip-card:hover .flip-card-inner {
  transform: rotateY(180deg);
}

.flip-card-front, .flip-card-back {
  position: absolute;
  width: 100%;
  height: 100%;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
}

.flip-card-back {
  transform: rotateY(180deg);
}

/* 进度条动画 */
.progress-bar {
  width: 100%;
  height: 4px;
  background-color: #f0f0f0;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #409EFF, #67C23A);
  border-radius: 2px;
  animation: progressMove 2s ease-in-out infinite;
}

@keyframes progressMove {
  0% {
    transform: translateX(-100%);
  }
  50% {
    transform: translateX(0%);
  }
  100% {
    transform: translateX(100%);
  }
}

/* 响应式动画优化 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* 移动端动画优化 */
@media (max-width: 768px) {
  .el-card:hover {
    transform: none;
  }
  
  .el-button:hover {
    transform: none;
  }
  
  .el-table tbody tr:hover {
    transform: none;
  }
}
</style>