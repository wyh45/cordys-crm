/// <reference types="vite/client" />

declare module '*.vue' {
  import { DefineComponent } from 'vue';

  // eslint-disable-next-line @typescript-eslint/no-empty-object-type
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

declare module '*.vue' {
  import type { Component } from 'vue';

  // Type-only declarations for named exports from Vue SFC scripts
  // These exports exist in <script setup lang="ts"> with "export interface"
  // but the generic .vue module declaration doesn't expose them.
  // Use "type" keyword so they merge as type declarations, not values.
  export type CrmTagGroupProps = any;
  export type CrmPaginationProps = any;
  export type CrmPopConfirmProps = any;
  export type UserTagSelectorProps = any;
  export type Description = any;
  export type FormulaFormCreateField = any;
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_DEV_DOMAIN: string; // 开发环境域名
}

declare module 'xml-beautify' {
  export default class xmlBeautify {
    beautify: (xml: string) => string;
  }
}
