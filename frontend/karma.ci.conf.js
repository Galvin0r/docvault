const base = require("./karma.conf.js");

module.exports = (config) => {
  base(config);
  config.set({
    browsers: ["ChromeHeadless"],
    singleRun: true,
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    coverageReporter: {
      reporters: [
        { type: "html" },
        { type: "text-summary" },
        { type: "lcovonly" }
      ],
      includeAllSources: true,
      check: {
        global: {
          statements: 70,
          branches: 60,
          functions: 65,
          lines: 70
        },
        each: {
          excludes: [
            'src/app/menu/auth-layout/**',
            'src/app/menu/main-layout/**',
            'src/app/menu/home/**',
            'src/app/menu/top-menu/**'
          ]
        }
      }
    }
  });
};
