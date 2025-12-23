/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  reactStrictMode: true,

  // Версионирование: переменные доступны в build-time
  env: {
    NEXT_PUBLIC_APP_VERSION: process.env.npm_package_version || '0.1.0',
    NEXT_PUBLIC_BUILD_TIME: new Date().toISOString(),
    NEXT_PUBLIC_GIT_COMMIT:
      process.env.VERCEL_GIT_COMMIT_SHA?.substring(0, 7) ||
      process.env.GITHUB_SHA?.substring(0, 7) ||
      'local',
  },

  // Build ID на основе Git commit
  generateBuildId: async () => {
    return (
      process.env.VERCEL_GIT_COMMIT_SHA?.substring(0, 7) ||
      process.env.GITHUB_SHA?.substring(0, 7) ||
      'development'
    );
  },
};

module.exports = nextConfig;
