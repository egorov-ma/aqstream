/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  reactStrictMode: true,

  // Compiler optimizations для INP
  compiler: {
    // Удалять console.* в production
    removeConsole: process.env.NODE_ENV === 'production',
  },

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

  // Проксирование API запросов на Gateway
  async rewrites() {
    const gatewayUrl = process.env.NEXT_PUBLIC_GATEWAY_URL || 'http://localhost:8080';

    return [
      {
        source: '/api/:path*',
        destination: `${gatewayUrl}/api/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
