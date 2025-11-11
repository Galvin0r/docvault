const base = require('./karma.conf.js');

module.exports = (config) => {
  base(config);
  config.set({
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    reporters: ['progress', 'coverage'],
    coverageReporter: {
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        { type: 'lcovonly' }
      ],
      includeAllSources: false
    },
    singleRun: true,
    browsers: ['ChromeHeadless']
  });
};
