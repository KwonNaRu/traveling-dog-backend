const nextJest = require("next/jest");

const createJestConfig = nextJest({
    dir: "./", // Next.js의 기본 디렉토리
});

const customJestConfig = {
    testEnvironment: "jest-environment-jsdom",
    setupFilesAfterEnv: ["<rootDir>/jest.setup.js"], // Jest 설정 파일
    moduleNameMapper: {
        // 경로 별칭 처리
        "^@components/(.*)$": "<rootDir>/src/components/$1",
        "^@app/(.*)$": "<rootDir>/src/app/$1", // `src/app` 별칭
    },
    testPathIgnorePatterns: ["<rootDir>/node_modules/", "<rootDir>/.next/"], // 테스트 제외 경로
    transform: {
        "^.+\\.(js|jsx|ts|tsx)$": ["ts-jest", { tsconfig: "<rootDir>/tsconfig.json" }],
    },
};

module.exports = createJestConfig(customJestConfig);
