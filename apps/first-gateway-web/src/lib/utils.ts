import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export const TOKEN_KEY = 'fg_access_token'
export const API_KEY_STORAGE = 'fg_default_api_key'
