import type { Directive } from 'vue'

const permission: Directive = {
  mounted(el, binding) {
    const allowedRoles: string[] = binding.value || []
    if (allowedRoles.length === 0) return

    const role = localStorage.getItem('role')
    if (!role || !allowedRoles.includes(role)) {
      el.parentNode?.removeChild(el)
    }
  },
}

export default permission
