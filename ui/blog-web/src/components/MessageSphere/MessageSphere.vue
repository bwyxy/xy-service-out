<!-- App.vue -->
<template>
  <div class="sphere-container">
    <div ref="canvasContainer" class="canvas-wrapper"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, toRefs, watch } from 'vue';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { CSS2DRenderer, CSS2DObject } from 'three/examples/jsm/renderers/CSS2DRenderer.js';

const props = defineProps({
  comments: {
    type: Array,
    default() {
      return []
    }
  }
})
const { comments } = toRefs(props)
// ---------- 留言数据 (每个球形节点包含头像信息 + 留言内容) ----------
const messages = ref([
  {
    id: 1,
    name: '林星野',
    avatarEmoji: '🌌',
    avatarColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    message: '星光不问赶路人，时光不负有心人。✨',
    timestamp: '2025-03-15'
  },
  {
    id: 2,
    name: '苏小棠',
    avatarEmoji: '🌸',
    avatarColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    message: '每一个不曾起舞的日子，都是对生命的辜负。💃',
    timestamp: '2025-03-14'
  },
  {
    id: 3,
    name: '陆远舟',
    avatarEmoji: '⛵',
    avatarColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    message: '长风破浪会有时，直挂云帆济沧海。🌊',
    timestamp: '2025-03-13'
  },
  {
    id: 4,
    name: '楚晴雪',
    avatarEmoji: '❄️',
    avatarColor: 'linear-gradient(135deg, #a6c1ee 0%, #fbc2eb 100%)',
    message: '保持热爱，奔赴山海，忠于自己，热爱生活。🏔️',
    timestamp: '2025-03-12'
  },
  {
    id: 5,
    name: '谢知非',
    avatarEmoji: '🎻',
    avatarColor: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
    message: '音乐是灵魂的避难所，文字是思想的栖息地。📖',
    timestamp: '2025-03-11'
  },
  {
    id: 6,
    name: '江辰希',
    avatarEmoji: '🌊',
    avatarColor: 'linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%)',
    message: '向海风许愿，在山海相见。🌊✨',
    timestamp: '2025-03-10'
  },
  {
    id: 7,
    name: '白鹿鸣',
    avatarEmoji: '🦌',
    avatarColor: 'linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%)',
    message: '且听风吟，静待花开。🌼',
    timestamp: '2025-03-09'
  },
  {
    id: 8,
    name: '温如玉',
    avatarEmoji: '💎',
    avatarColor: 'linear-gradient(135deg, #fccb90 0%, #d57eeb 100%)',
    message: '温润而泽，谦谦君子，卑以自牧也。🎋',
    timestamp: '2025-03-08'
  },
  {
    id: 9,
    name: '沈千尘',
    avatarEmoji: '🍂',
    avatarColor: 'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
    message: '秋天的梦，是落叶与风的诗。🍁',
    timestamp: '2025-03-07'
  },
  {
    id: 10,
    name: '叶扶苏',
    avatarEmoji: '🌿',
    avatarColor: 'linear-gradient(135deg, #c0f0e8 0%, #9fd3c7 100%)',
    message: '草木有本心，何求美人折。🌱',
    timestamp: '2025-03-06'
  },
  {
    id: 11,
    name: '云隐',
    avatarEmoji: '☁️',
    avatarColor: 'linear-gradient(135deg, #e0eafc 0%, #cfdef3 100%)',
    message: '云深不知处，只在此山中。⛰️',
    timestamp: '2025-03-05'
  },
  {
    id: 12,
    name: '墨小鱼',
    avatarEmoji: '🐟',
    avatarColor: 'linear-gradient(135deg, #fbc2eb 0%, #a6c1ee 100%)',
    message: '子非鱼，安知鱼之乐？🐠',
    timestamp: '2025-03-04'
  },
  {
    id: 13,
    name: '花满楼',
    avatarEmoji: '🌺',
    avatarColor: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    message: '满楼花香，不问归期。🌼',
    timestamp: '2025-03-03'
  },
  {
    id: 14,
    name: '柳明烟',
    avatarEmoji: '🍃',
    avatarColor: 'linear-gradient(135deg, #b5f4a6 0%, #6cdb87 100%)',
    message: '春风十里，不如你。🌿',
    timestamp: '2025-03-02'
  },
  {
    id: 15,
    name: '南宫逸',
    avatarEmoji: '⚔️',
    avatarColor: 'linear-gradient(135deg, #e2b0ff 0%, #9f7aea 100%)',
    message: '剑未佩妥，出门已是江湖。🗡️',
    timestamp: '2025-03-01'
  },
  {
    id: 16,
    name: '百里屠苏',
    avatarEmoji: '🔥',
    avatarColor: 'linear-gradient(135deg, #ff8c00 0%, #ff2e00 100%)',
    message: '心之所向，素履以往。🔥',
    timestamp: '2025-02-28'
  }
]);

watch(() => comments.value, () => {
  if (comments.value.length > 0) {
    const positions = getUniformPointsOnSphere(pointsCount, sphereRadius);
    css2dObjects = [];

    comments.value.forEach((msg, idx) => {
      const domElement = createMessageNode(msg);
      const css2DLabel = new CSS2DObject(domElement);
      css2DLabel.position.copy(positions[idx]);
      scene.add(css2DLabel);
      css2dObjects.push(css2DLabel);
    });
  }
}, { deep: true })

// ---------- Three.js 相关变量 ----------
const canvasContainer = ref(null);
let scene, camera, labelRenderer, controls, css2dObjects = [];
let renderer; // WebGL渲染器(背景装饰，现已透明)
let animationId;
let hoverCount = 0;          // 悬停计数器，用于控制自动旋转暂停/恢复
const eventHandlers = [];    // 存储事件解绑函数

// 球面参数
const sphereRadius = 3.6;
const pointsCount = messages.value.length;

// 均匀球面分布算法 (Fibonacci 球体算法，保证视觉均匀)
function getUniformPointsOnSphere(numPoints, radius) {
  const points = [];
  const phiGoldenRatio = Math.PI * (3 - Math.sqrt(5));
  for (let i = 0; i < numPoints; i++) {
    const y = 1 - (i / (numPoints - 1)) * 2;
    const radiusAtY = Math.sqrt(1 - y*y);
    const theta = i * phiGoldenRatio * 2;
    const x = Math.cos(theta) * radiusAtY;
    const z = Math.sin(theta) * radiusAtY;
    const px = x * radius;
    const py = y * radius;
    const pz = z * radius;
    points.push(new THREE.Vector3(px, py, pz));
  }
  return points;
}

// 创建单个留言节点的 DOM 结构 (圆形/卡片双状态)
function createMessageNode(messageData) {
  const container = document.createElement('div');
  container.className = 'message-sphere-node';

  // 圆形头像区域 (未触摸时的状态)
  const avatarDiv = document.createElement('div');
  avatarDiv.className = 'avatar-circle';
  // avatarDiv.style.background = messageData.avatarColor;
  const avatarEmojiSpan = document.createElement('span');
  avatarEmojiSpan.className = 'avatar-emoji';
  avatarEmojiSpan.textContent = messageData.avatarEmoji || '👤';
  const nameHint = document.createElement('span');
  // nameHint.className = 'avatar-name';
  // nameHint.textContent = messageData.name.slice(0, 2);

  avatarDiv.appendChild(avatarEmojiSpan);
  avatarDiv.appendChild(nameHint);

  // 卡片区域 (鼠标触摸时显示留言信息)
  const cardDiv = document.createElement('div');
  cardDiv.className = 'message-card';

  const cardHeader = document.createElement('div');
  cardHeader.className = 'card-header';
  const cardAvatarSmall = document.createElement('div');
  cardAvatarSmall.className = 'card-avatar-small';
  cardAvatarSmall.style.background = ''
  cardAvatarSmall.textContent = messageData.avatarEmoji || '👤';
  const cardName = document.createElement('div');
  cardName.className = 'card-name';
  cardName.textContent = messageData.nickname || '';

  const ipAddress = document.createElement('div');
  ipAddress.className = 'ip-address';
  ipAddress.textContent = messageData.ipAddress || ''

  cardHeader.appendChild(cardAvatarSmall);
  cardHeader.appendChild(cardName);
  cardHeader.appendChild(ipAddress);

  const cardMessage = document.createElement('div');
  cardMessage.className = 'card-message';
  cardMessage.textContent = messageData.content || '';

  const cardFooter = document.createElement('div');
  cardFooter.className = 'card-footer';
  const timeIcon = document.createElement('span');
  timeIcon.textContent = '📅 ';
  const timeSpan = document.createElement('span');
  timeSpan.textContent = messageData.createTime;
  cardFooter.appendChild(timeIcon);
  cardFooter.appendChild(timeSpan);

  cardDiv.appendChild(cardHeader);
  cardDiv.appendChild(cardMessage);
  cardDiv.appendChild(cardFooter);

  container.appendChild(avatarDiv);
  container.appendChild(cardDiv);

  return container;
}

// 为所有节点绑定鼠标/触摸事件 (用于控制自动旋转暂停/恢复)
function bindHoverEvents() {
  // 增加计数函数
  const incrementHover = () => {
    hoverCount++;
    if (controls && hoverCount === 1) {
      controls.autoRotate = false;
    }
  };
  // 减少计数函数
  const decrementHover = () => {
    hoverCount--;
    if (controls && hoverCount === 0) {
      controls.autoRotate = true;
    }
    // 防止负数
    if (hoverCount < 0) hoverCount = 0;
  };

  css2dObjects.forEach(obj => {
    const domEl = obj.element;
    if (!domEl) return;

    // 鼠标事件
    const onMouseEnter = () => incrementHover();
    const onMouseLeave = () => decrementHover();

    // 触摸事件 (移动端支持)
    const onTouchStart = (e) => {
      e.preventDefault();  // 避免页面滚动，提升体验
      incrementHover();
    };
    const onTouchEnd = (e) => {
      e.preventDefault();
      decrementHover();
    };
    const onTouchCancel = (e) => {
      e.preventDefault();
      decrementHover();
    };

    domEl.addEventListener('mouseenter', onMouseEnter);
    domEl.addEventListener('mouseleave', onMouseLeave);
    domEl.addEventListener('touchstart', onTouchStart, { passive: false });
    domEl.addEventListener('touchend', onTouchEnd);
    domEl.addEventListener('touchcancel', onTouchCancel);

    // 存储以便清理
    eventHandlers.push({
      el: domEl,
      mouseenter: onMouseEnter,
      mouseleave: onMouseLeave,
      touchstart: onTouchStart,
      touchend: onTouchEnd,
      touchcancel: onTouchCancel
    });
  });
}

// 解绑所有事件
function unbindHoverEvents() {
  eventHandlers.forEach(handler => {
    const { el, mouseenter, mouseleave, touchstart, touchend, touchcancel } = handler;
    el.removeEventListener('mouseenter', mouseenter);
    el.removeEventListener('mouseleave', mouseleave);
    el.removeEventListener('touchstart', touchstart);
    el.removeEventListener('touchend', touchend);
    el.removeEventListener('touchcancel', touchcancel);
  });
  eventHandlers.length = 0;
  hoverCount = 0;
  if (controls) controls.autoRotate = true; // 清理时恢复自动旋转状态
}

// 初始化Three场景 (去背景 + 自动旋转)
function initThree() {
  if (!canvasContainer.value) return;

  const wid = canvasContainer.value.clientWidth
  const hgt = canvasContainer.value.clientHeight
  // 1. 场景 (背景完全透明，露出CSS body渐变)
  scene = new THREE.Scene();
  scene.background = null;   // 关键: 去掉Three.js场景背景

  // 装饰元素: 星空粒子 (不破坏透明背景)
  const starGeometry = new THREE.BufferGeometry();
  const starCount = 1200;
  const starPositions = new Float32Array(starCount * 3);
  for (let i = 0; i < starCount; i++) {
    starPositions[i*3] = (Math.random() - 0.5) * 2000;
    starPositions[i*3+1] = (Math.random() - 0.5) * 1000;
    starPositions[i*3+2] = (Math.random() - 0.5) * 400 - 150;
  }
  starGeometry.setAttribute('position', new THREE.BufferAttribute(starPositions, 3));
  const starMaterial = new THREE.PointsMaterial({ color: 0xffffff, size: 0.3, transparent: true, opacity: 0.5 });
  const stars = new THREE.Points(starGeometry, starMaterial);
  scene.add(stars);

  // 微妙的半透明线框球体 (增强球形感，非背景)
  const wireframeSphere = new THREE.Mesh(
      new THREE.SphereGeometry(sphereRadius + 0.03, 32, 16),
      new THREE.MeshBasicMaterial({ color: 0x6c63ff, wireframe: true, transparent: true, opacity: 0.12 })
  );
  scene.add(wireframeSphere);

  // 球体周围光晕粒子
  const glowGeometry = new THREE.BufferGeometry();
  const glowCount = 600;
  const glowPositions = new Float32Array(glowCount * 3);
  for (let i = 0; i < glowCount; i++) {
    const theta = Math.random() * Math.PI * 2;
    const phi = Math.acos(2 * Math.random() - 1);
    const r = sphereRadius + 0.08;
    const x = r * Math.sin(phi) * Math.cos(theta);
    const y = r * Math.sin(phi) * Math.sin(theta);
    const z = r * Math.cos(phi);
    glowPositions[i*3] = x;
    glowPositions[i*3+1] = y;
    glowPositions[i*3+2] = z;
  }
  glowGeometry.setAttribute('position', new THREE.BufferAttribute(glowPositions, 3));
  const glowMaterial = new THREE.PointsMaterial({ color: 0x88aaff, size: 0.06, transparent: true, opacity: 0.35 });
  const glowPoints = new THREE.Points(glowGeometry, glowMaterial);
  scene.add(glowPoints);

  // 2. 相机
  camera = new THREE.PerspectiveCamera(45, wid / hgt, 0.1, 1000);
  camera.position.set(6, 3, 8);
  camera.lookAt(0, 0, 0);

  // 3. WebGL渲染器 (透明背景，让CSS背景透出)
  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true }); // alpha 开启透明
  renderer.setSize(wid, hgt);
  renderer.setClearColor(0x000000, 0); // 完全透明
  renderer.setPixelRatio(window.devicePixelRatio);
  canvasContainer.value.appendChild(renderer.domElement);

  // 4. CSS2渲染器 (文字和卡片，天生透明)
  labelRenderer = new CSS2DRenderer();
  labelRenderer.setSize(wid, hgt);
  labelRenderer.domElement.style.position = 'absolute';
  labelRenderer.domElement.style.top = '0px';
  labelRenderer.domElement.style.left = '0px';
  labelRenderer.domElement.style.pointerEvents = 'auto';
  labelRenderer.domElement.style.zIndex = '10';
  canvasContainer.value.appendChild(labelRenderer.domElement);

  // 5. 轨道控制 + 自动旋转核心配置
  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.dampingFactor = 0.05;
  controls.rotateSpeed = 1.2;
  controls.zoomSpeed = 1.2;
  controls.enableZoom = true;
  controls.autoRotate = true;       // 默认自动旋转
  controls.autoRotateSpeed = 1.6;   // 旋转速度舒适
  controls.enablePan = false;
  controls.target.set(0, 0, 0);

  // 6. 生成球面节点位置
  // const positions = getUniformPointsOnSphere(pointsCount, sphereRadius);
  // css2dObjects = [];
  //
  // messages.value.forEach((msg, idx) => {
  //   const domElement = createMessageNode(msg);
  //   const css2DLabel = new CSS2DObject(domElement);
  //   css2DLabel.position.copy(positions[idx]);
  //   scene.add(css2DLabel);
  //   css2dObjects.push(css2DLabel);
  // });

  // 7. 添加柔和光照提升立体感 (不影响背景)
  const ambientLight = new THREE.AmbientLight(0x404060);
  scene.add(ambientLight);
  const pointLight = new THREE.PointLight(0x88aaff, 0.7);
  pointLight.position.set(2, 3, 4);
  scene.add(pointLight);
  const backLight = new THREE.PointLight(0xff99cc, 0.4);
  backLight.position.set(-2, -1, -3);
  scene.add(backLight);

  // 8. 绑定悬停事件 (实现“鼠标触碰自动旋转暂停”)
  bindHoverEvents();
}

// 动画循环
function animate() {
  animationId = requestAnimationFrame(animate);
  if (controls) {
    controls.update(); // 自动旋转由 controls.autoRotate 标志控制
  }
  // 让星星粒子缓慢旋转，增加动态感
  const starsObj = scene?.children.find(child => child.isPoints && child.material && child.material.color.getHex() === 0xffffff);
  if (starsObj) {
    starsObj.rotation.y += 0.0005;
    starsObj.rotation.x += 0.0003;
  }
  if (renderer && labelRenderer && scene && camera) {
    renderer.render(scene, camera);
    labelRenderer.render(scene, camera);
  }
}

// 窗口适配
function handleResize() {
  if (!camera || !renderer || !labelRenderer) return;

  const wid = canvasContainer.value.clientWidth
  const hgt = canvasContainer.value.clientHeight

  camera.aspect = wid / hgt;
  camera.updateProjectionMatrix();
  renderer.setSize(wid, hgt);
  labelRenderer.setSize(wid, hgt);
}

// 清理所有资源
function cleanup() {
  if (animationId) {
    cancelAnimationFrame(animationId);
    animationId = null;
  }
  window.removeEventListener('resize', handleResize);

  // 解绑所有悬停事件，并恢复自动旋转标志
  unbindHoverEvents();

  if (controls) {
    controls.dispose();
    controls = null;
  }
  if (renderer) {
    renderer.dispose();
    if (renderer.domElement && renderer.domElement.parentNode) {
      renderer.domElement.parentNode.removeChild(renderer.domElement);
    }
    renderer = null;
  }
  if (labelRenderer && labelRenderer.domElement && labelRenderer.domElement.parentNode) {
    labelRenderer.domElement.parentNode.removeChild(labelRenderer.domElement);
    labelRenderer = null;
  }
  if (scene) {
    // 清理 CSS2D 对象
    if (css2dObjects) {
      css2dObjects.forEach(obj => {
        if (obj.parent) scene.remove(obj);
        if (obj.element) obj.element.remove(); // 清理DOM
      });
      css2dObjects = [];
    }
    scene = null;
  }
}

onMounted(() => {
  initThree();
  animate();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  cleanup();
});
</script>

<style>
/* 全局容器 */
.sphere-container {
  position: relative;
  width: 100%;
  height: 840px;
  overflow: hidden;
  background: transparent; /* 完全透明，让body渐变显示出来 */
  margin-top: 100px;
  display: flex;
  align-items: flex-end;
}
.canvas-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 800px;
  z-index: 1;
}
.info-panel {
  position: absolute;
  bottom: 20px;
  left: 20px;
  z-index: 20;
  background: rgba(10, 20, 40, 0.65);
  backdrop-filter: blur(12px);
  border-radius: 28px;
  padding: 12px 24px;
  pointer-events: none;
  border-left: 3px solid #6c63ff;
  box-shadow: 0 8px 20px rgba(0,0,0,0.3);
  font-weight: 500;
}
.info-panel h1 {
  font-size: 1.2rem;
  margin: 0;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #fff, #a0c0ff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.info-panel p {
  font-size: 0.75rem;
  margin: 5px 0 0;
  opacity: 0.9;
}
.stats {
  display: flex;
  gap: 16px;
  margin-top: 6px;
  font-size: 0.7rem;
  color: #ccd6f6;
}
.instruction {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 20;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(8px);
  border-radius: 40px;
  padding: 8px 16px;
  font-size: 0.7rem;
  font-family: monospace;
  pointer-events: none;
  color: #bbb;
}

/* ------------- 球形节点核心样式: 圆形头像 ↔ 留言卡片 平滑过渡 ------------- */
.message-sphere-node {
  width: 50px;
  height: 50px;
  position: relative;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.2, 0.9, 0.4, 1.1);
  transform-style: preserve-3d;
  filter: drop-shadow(0 10px 15px rgba(0,0,0,0.3));
}

/* 圆形头像样式 (默认) */
.avatar-circle {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, .1);
  backdrop-filter: blur(10px);
  box-sizing: border-box;
  border: 1.3px solid rgba(255, 255, 255, .2);
  transition: all 0.3s ease;
  backface-visibility: hidden;
  z-index: 2;
}
.avatar-emoji {
  font-size: 24px;
  //filter: drop-shadow(2px 2px 4px rgba(0,0,0,0.2));
}
.avatar-name {
  font-size: 16px;
  font-weight: bold;
  background: rgba(0,0,0,0.4);
  padding: 2px 8px;
  border-radius: 20px;
  margin-top: 6px;
  color: white;
  letter-spacing: 1px;
}

/* 卡片样式 (初始隐藏) */
.message-card {
  position: absolute;
  top: 0;
  left: 0;
  width: 230px;
  background: rgba(22, 28, 46, 0.96);
  backdrop-filter: blur(14px);
  border-radius: 24px;
  padding: 14px;
  box-shadow: 0 20px 35px -12px rgba(0,0,0,0.5), 0 0 0 1px rgba(255,255,255,0.15);
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.25s ease, visibility 0.25s, transform 0.2s;
  transform: scale(0.92);
  pointer-events: none;
  z-index: 10;
  color: #f0f3fa;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  border-bottom: 1px solid rgba(255,255,255,0.2);
  padding-bottom: 8px;
}
.card-avatar-small {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: #a463f5;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.card-name {
  font-weight: bold;
  font-size: 18px;
  letter-spacing: 0.5px;
  background: linear-gradient(120deg, #fff, #b2c9ff);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.card-message {
  font-size: 20px;
  line-height: 1.4;
  margin: 12px 4px;
  word-break: break-word;
  color: #e0e7ff;
  font-weight: 400;
}
.card-footer {
  font-size: 16px;
  color: #9aa9c7;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 5px;
  justify-content: flex-end;
  border-top: 1px solid rgba(255,255,255,0.1);
  padding-top: 8px;
}

/* 悬浮效果: 圆形缩小隐藏，卡片完全展开 */
.message-sphere-node:hover {
  width: 250px;
  height: auto;
  min-height: 130px;
  z-index: 100;
  filter: drop-shadow(0 15px 25px rgba(0,0,0,0.5));
}
.message-sphere-node:hover .avatar-circle {
  opacity: 0;
  visibility: hidden;
  transform: scale(0.6);
  transition: all 0.2s;
}
.message-sphere-node:hover .message-card {
  opacity: 1;
  visibility: visible;
  transform: scale(1);
  pointer-events: auto;
  transition-delay: 0.05s;
}

/* 卡片弹出位置微调 (向左展开，避免超出视觉) */
.message-card {
  left: 50%;
  transform-origin: left center;
  transform: scale(0.92) translateX(-10%);
}
.message-sphere-node:hover .message-card {
  transform: scale(1) translateX(0%);
  left: 100%;
  top: -10px;
  margin-left: -240px;
}

/* 移动端适配 */
@media (max-width: 600px) {
  .message-sphere-node:hover {
    width: 220px;
  }
  .message-card {
    width: 210px;
  }
  .message-sphere-node:hover .message-card {
    margin-left: -200px;
  }
}

/* 节点光晕微效果 */
.message-sphere-node::before {
  content: '';
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(100,108,255,0.4) 0%, rgba(100,108,255,0) 70%);
  opacity: 0;
  transition: opacity 0.3s;
  pointer-events: none;
  z-index: -1;
}
.message-sphere-node:hover::before {
  opacity: 1;
}

.ip-address {
  flex: 1;
  font-size: 14px;
  letter-spacing: 1px;
  display: flex;
  justify-content: flex-end;
}
</style>