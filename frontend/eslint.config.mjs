import { dirname } from "path";
import { fileURLToPath } from "url";
import { FlatCompat } from "@eslint/eslintrc";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const compat = new FlatCompat({
    baseDirectory: __dirname,
});

const eslintConfig = [
    ...compat.extends("next/core-web-vitals", "next/typescript"),
    {
        files: ["**/*.ts", "**/*.tsx", "**/*.js", "**/*.jsx"],
        plugins: ["prettier"], // Prettier 플러그인 추가
        rules: {
            "prettier/prettier": [
                "error",
                {
                    semi: false, // 세미콜론 사용 안 함
                    singleQuote: false, // 쌍따옴표 사용
                    trailingComma: "es5", // 여러 줄일 경우 마지막에 콤마
                },
            ],
            "no-unused-vars": ["warn"], // 사용되지 않는 변수 경고
            "no-console": ["warn"], // console.log 경고
            // TypeScript 스타일 관련 규칙
            "@typescript-eslint/explicit-function-return-type": "warn", // 함수 반환 타입 명시 요구
            "@typescript-eslint/no-unused-vars": ["warn", { argsIgnorePattern: "^_" }], // 사용하지 않는 변수 경고
            "@typescript-eslint/no-explicit-any": "warn", // 'any' 타입 사용 제한
            "@typescript-eslint/consistent-type-definitions": ["error", "interface"], // interface 사용 권장
            "@typescript-eslint/no-inferrable-types": "warn", // 추론 가능한 타입 명시 방지
            "@typescript-eslint/member-delimiter-style": [
                "error",
                {
                    multiline: {
                        delimiter: "none", // 멤버 구분 기호 제거
                        requireLast: true,
                    },
                    singleline: {
                        delimiter: "comma", // 한 줄일 경우 콤마 사용
                        requireLast: false,
                    },
                },
            ],
        },
    },
];

export default eslintConfig;
